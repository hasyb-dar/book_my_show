package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.anthropic.core.JsonValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.scaler.bms.may2026.dto.CreateScreenRequestDTO;
import com.example.scaler.bms.may2026.dto.CreateShowRequestDTO;
import com.example.scaler.bms.may2026.model.Booking;
import com.example.scaler.bms.may2026.model.City;
import com.example.scaler.bms.may2026.model.Feature;
import com.example.scaler.bms.may2026.model.Movie;
import com.example.scaler.bms.may2026.model.Screen;
import com.example.scaler.bms.may2026.model.Show;
import com.example.scaler.bms.may2026.model.Theatre;
import com.example.scaler.bms.may2026.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns natural-language requests into real calls against the catalog
 * (City/Movie/Theatre/Screen/Show) and booking/user services, using
 * Claude's tool-use feature.
 */
@Service
@Slf4j
public class AssistantService {

    @Autowired
    private BookingServiceImpl bookingServiceImpl;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private CityServiceImpl cityServiceImpl;

    @Autowired
    private MovieServiceImpl movieServiceImpl;

    @Autowired
    private TheatreServiceImpl theatreServiceImpl;

    @Autowired
    private ScreenServiceImpl screenServiceImpl;

    @Autowired
    private ShowServiceImpl showServiceImpl;

    private final AnthropicClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantService() {
        this.client = AnthropicOkHttpClient.fromEnv();
    }

    /**
     * Package-private: lets tests inject a mock AnthropicClient instead of
     * building a real one from ANTHROPIC_API_KEY. Not used by Spring, which
     * always picks the public no-arg constructor above.
     */
    AssistantService(AnthropicClient client) {
        this.client = client;
    }

    public String handleMessage(String userMessage) {

        List<MessageParam> conversation = new ArrayList<>();
        conversation.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(userMessage)
                .build());

        // Agentic loop: keep calling Claude until it stops asking for tools.
        for (int turn = 0; turn < 5; turn++) {

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(Model.CLAUDE_SONNET_4_6)
                    .maxTokens(1024L)
                    .system(SYSTEM_PROMPT)
                    .messages(conversation)
                    .tools(buildTools())
                    .build();

            Message response = client.messages().create(params);

            // Echo Claude's own turn back into the conversation history.
            // response.toParam() converts the response Message into the
            // MessageParam shape the next request needs.
            conversation.add(response.toParam());

            List<ToolUseBlock> toolUses = new ArrayList<>();
            StringBuilder textReply = new StringBuilder();

            for (ContentBlock block : response.content()) {
                block.text().ifPresent(t -> textReply.append(t.text()));
                block.toolUse().ifPresent(toolUses::add);
            }

            // No tool calls left -> Claude gave its final answer.
            if (toolUses.isEmpty()) {
                return textReply.toString();
            }

            // Execute every requested tool and feed results back.
            List<ContentBlockParam> results = new ArrayList<>();
            for (ToolUseBlock toolUse : toolUses) {
                String result = executeTool(toolUse.name(), objectMapper.valueToTree(toolUse._input()));
                results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                        .toolUseId(toolUse.id())
                        .content(result)
                        .build()));
            }

            conversation.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(results)
                    .build());
        }

        return "Sorry, I couldn't complete that request in a reasonable number of steps.";
    }

    // ---- Tool execution: maps Claude's tool calls to your real service methods ----

    private String executeTool(String toolName, JsonNode input) {
        try {
            switch (toolName) {

                // ---- Booking / User ----

                case "create_booking": {
                    Long showId = input.get("showId").asLong();
                    Long userId = input.get("userId").asLong();
                    List<String> seatNumbers = new ArrayList<>();
                    input.get("seatNumbers").forEach(n -> seatNumbers.add(n.asText()));

                    Booking booking = bookingServiceImpl.createBooking(seatNumbers, showId, userId);
                    return String.format(
                            "Booking created. id=%d, status=%s, totalAmount=%.2f",
                            booking.getId(), booking.getBookingStatus(), booking.getTotalAmount());
                }

                case "get_ticket": {
                    Long bookingId = input.get("bookingId").asLong();
                    Booking booking = bookingServiceImpl.getBookingById(bookingId);
                    return String.format(
                            "Ticket id=%d, status=%s, totalAmount=%.2f, bookedBy=%s",
                            booking.getId(), booking.getBookingStatus(),
                            booking.getTotalAmount(), booking.getCreatedBy().getEmail());
                }

                case "register_user": {
                    String email = input.get("email").asText();
                    String password = input.get("password").asText();
                    User user = userServiceImpl.registerUser(email, password);
                    return String.format("User registered. id=%d, email=%s", user.getId(), user.getEmail());
                }

                case "login_user": {
                    String email = input.get("email").asText();
                    String password = input.get("password").asText();
                    boolean success = userServiceImpl.login(email, password);
                    return success ? "Login successful." : "Login failed.";
                }

                // ---- City ----

                case "create_city": {
                    String name = input.get("name").asText();
                    City city = cityServiceImpl.createCity(name);
                    return String.format("City created. id=%d, name=%s", city.getId(), city.getName());
                }

                // ---- Movie ----

                case "create_movie": {
                    String name = input.get("name").asText();
                    String rating = input.has("rating") ? input.get("rating").asText() : null;
                    Long duration = input.get("duration").asLong();

                    List<Feature> features = input.has("features")
                            ? Arrays.asList(objectMapper.convertValue(input.get("features"), Feature[].class))
                            : List.of();

                    List<String> languages = input.has("languages")
                            ? Arrays.asList(objectMapper.convertValue(input.get("languages"), String[].class))
                            : List.of();

                    List<Long> actorIds = input.has("actorIds")
                            ? Arrays.asList(objectMapper.convertValue(input.get("actorIds"), Long[].class))
                            : null;

                    Movie movie = movieServiceImpl.createMovie(name, rating, features, languages, duration, actorIds);
                    return String.format("Movie created. id=%d, name=%s", movie.getId(), movie.getName());
                }

                case "get_movie": {
                    Long movieId = input.get("movieId").asLong();
                    Movie movie = movieServiceImpl.getMovieById(movieId);
                    return String.format(
                            "Movie id=%d, name=%s, rating=%s, durationMinutes=%d",
                            movie.getId(), movie.getName(), movie.getRating(), movie.getDuration());
                }

                // ---- Theatre ----

                case "create_theatre": {
                    String name = input.get("name").asText();
                    Long cityId = input.get("cityId").asLong();
                    Theatre theatre = theatreServiceImpl.createTheatre(name, cityId);
                    return String.format("Theatre created. id=%d, name=%s", theatre.getId(), theatre.getName());
                }

                case "get_theatre": {
                    Long theatreId = input.get("theatreId").asLong();
                    Theatre theatre = theatreServiceImpl.getTheatreById(theatreId);
                    return String.format(
                            "Theatre id=%d, name=%s, city=%s",
                            theatre.getId(), theatre.getName(),
                            theatre.getCity() != null ? theatre.getCity().getName() : "unknown");
                }

                // ---- Screen ----

                case "create_screen": {
                    CreateScreenRequestDTO requestDTO = objectMapper.treeToValue(input, CreateScreenRequestDTO.class);
                    Screen screen = screenServiceImpl.createScreen(requestDTO);
                    return String.format(
                            "Screen created. id=%d, name=%s, totalSeats=%d",
                            screen.getId(), screen.getName(),
                            screen.getSeats() == null ? 0 : screen.getSeats().size());
                }

                case "get_screen": {
                    Long screenId = input.get("screenId").asLong();
                    Screen screen = screenServiceImpl.getScreenById(screenId);
                    return String.format(
                            "Screen id=%d, name=%s, totalSeats=%d",
                            screen.getId(), screen.getName(),
                            screen.getSeats() == null ? 0 : screen.getSeats().size());
                }

                // ---- Show ----

                case "create_show": {
                    CreateShowRequestDTO requestDTO = objectMapper.treeToValue(input, CreateShowRequestDTO.class);
                    Show show = showServiceImpl.createShow(requestDTO);
                    int totalSeats = showServiceImpl.getShowSeats(show).size();
                    return String.format(
                            "Show created. id=%d, movie=%s, totalSeats=%d",
                            show.getId(),
                            show.getMovie() != null ? show.getMovie().getName() : "unknown",
                            totalSeats);
                }

                case "get_show": {
                    Long showId = input.get("showId").asLong();
                    Show show = showServiceImpl.getShowById(showId);
                    int totalSeats = showServiceImpl.getShowSeats(show).size();
                    return String.format(
                            "Show id=%d, movie=%s, startTime=%s, totalSeats=%d",
                            show.getId(),
                            show.getMovie() != null ? show.getMovie().getName() : "unknown",
                            show.getStartTime(), totalSeats);
                }

                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            // Return the error AS the tool result so Claude can react
            // (e.g. explain the problem to the user) instead of the whole
            // request blowing up with a 500.
            log.warn("Tool execution failed: {}", toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    // ---- Tool schemas: tell Claude what it's allowed to call and with what arguments ----

    private List<ToolUnion> buildTools() {
        return List.of(

                        // ---- Booking / User ----

                        Tool.builder()
                                .name("create_booking")
                                .description("Book seats for a show. Requires an existing showId and userId.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "showId": {"type": "integer"},
                                "userId": {"type": "integer"},
                                "seatNumbers": {
                                  "type": "array",
                                  "items": {"type": "string"}
                                }
                              },
                              "required": ["showId", "userId", "seatNumbers"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("get_ticket")
                                .description("Look up an existing booking/ticket by its ID.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "bookingId": {"type": "integer"}
                              },
                              "required": ["bookingId"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("register_user")
                                .description("Register a new user account with an email and password.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "email": {"type": "string"},
                                "password": {"type": "string"}
                              },
                              "required": ["email", "password"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("login_user")
                                .description("Log in an existing user with email and password.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "email": {"type": "string"},
                                "password": {"type": "string"}
                              },
                              "required": ["email", "password"]
                            }
                            """))
                                .build(),

                        // ---- City ----

                        Tool.builder()
                                .name("create_city")
                                .description("Create a new city that theatres can belong to.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "name": {"type": "string"}
                              },
                              "required": ["name"]
                            }
                            """))
                                .build(),

                        // ---- Movie ----

                        Tool.builder()
                                .name("create_movie")
                                .description("Create a new movie. actorIds is optional and references existing actors.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "name": {"type": "string"},
                                "rating": {"type": "string"},
                                "features": {
                                  "type": "array",
                                  "items": {"type": "string", "enum": ["DOLBY_ATMOS", "FOUR_D", "FIVE_D", "IMAX"]}
                                },
                                "languages": {
                                  "type": "array",
                                  "items": {"type": "string"}
                                },
                                "duration": {"type": "integer", "description": "Duration in minutes."},
                                "actorIds": {
                                  "type": "array",
                                  "items": {"type": "integer"}
                                }
                              },
                              "required": ["name", "duration"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("get_movie")
                                .description("Look up an existing movie by its ID.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "movieId": {"type": "integer"}
                              },
                              "required": ["movieId"]
                            }
                            """))
                                .build(),

                        // ---- Theatre ----

                        Tool.builder()
                                .name("create_theatre")
                                .description("Create a new theatre in an existing city.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "name": {"type": "string"},
                                "cityId": {"type": "integer"}
                              },
                              "required": ["name", "cityId"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("get_theatre")
                                .description("Look up an existing theatre by its ID.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "theatreId": {"type": "integer"}
                              },
                              "required": ["theatreId"]
                            }
                            """))
                                .build(),

                        // ---- Screen ----

                        Tool.builder()
                                .name("create_screen")
                                .description("""
                            Create a new screen inside an existing theatre, generating its \
                            seats from a list of seat rows. Each row produces seats numbered \
                            like A1, A2, ... based on rowLabel and numberOfSeats.""")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "theatreId": {"type": "integer"},
                                "name": {"type": "string"},
                                "features": {
                                  "type": "array",
                                  "items": {"type": "string", "enum": ["DOLBY_ATMOS", "FOUR_D", "FIVE_D", "IMAX"]}
                                },
                                "seatRows": {
                                  "type": "array",
                                  "items": {
                                    "type": "object",
                                    "properties": {
                                      "rowLabel": {"type": "string"},
                                      "numberOfSeats": {"type": "integer"},
                                      "seatType": {"type": "string", "enum": ["RECLINER", "GOLD", "SILVER"]}
                                    },
                                    "required": ["rowLabel", "numberOfSeats", "seatType"]
                                  }
                                }
                              },
                              "required": ["theatreId", "name", "seatRows"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("get_screen")
                                .description("Look up an existing screen by its ID.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "screenId": {"type": "integer"}
                              },
                              "required": ["screenId"]
                            }
                            """))
                                .build(),

                        // ---- Show ----

                        Tool.builder()
                                .name("create_show")
                                .description("""
                            Schedule a new show for a movie on a screen at a start time, with \
                            per-seat-type pricing. This generates the bookable seats for the show.""")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "movieId": {"type": "integer"},
                                "screenId": {"type": "integer"},
                                "startTime": {"type": "string", "description": "ISO-8601 date-time, e.g. 2026-09-10T18:30:00.000Z"},
                                "features": {
                                  "type": "array",
                                  "items": {"type": "string", "enum": ["DOLBY_ATMOS", "FOUR_D", "FIVE_D", "IMAX"]}
                                },
                                "seatPrices": {
                                  "type": "array",
                                  "items": {
                                    "type": "object",
                                    "properties": {
                                      "seatType": {"type": "string", "enum": ["RECLINER", "GOLD", "SILVER"]},
                                      "price": {"type": "number"}
                                    },
                                    "required": ["seatType", "price"]
                                  }
                                }
                              },
                              "required": ["movieId", "screenId", "startTime", "seatPrices"]
                            }
                            """))
                                .build(),

                        Tool.builder()
                                .name("get_show")
                                .description("Look up an existing show by its ID.")
                                .inputSchema(schema("""
                            {
                              "type": "object",
                              "properties": {
                                "showId": {"type": "integer"}
                              },
                              "required": ["showId"]
                            }
                            """))
                                .build()

                ).stream()
                .map(ToolUnion::ofTool)
                .toList();
    }

    private Tool.InputSchema schema(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            Object rawValue = objectMapper.convertValue(node, Object.class);
            return Tool.InputSchema.builder()
                    .putAdditionalProperty("raw", JsonValue.from(rawValue))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Invalid tool schema JSON", e);
        }
    }

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant for a movie ticket booking system.
            You can manage the full catalog (cities, theatres, screens, movies,
            shows) as well as user accounts and bookings, using the available
            tools.

            A screen's seats are generated when it is created, from the
            seatRows you provide. A show's bookable seats are generated when
            the show is created, from the screen it runs on, and its ticket
            prices come from the seatPrices you provide at that point.

            Ask for any missing required information before calling a tool,
            rather than guessing values (e.g. don't invent a cityId, screenId,
            or price). Keep replies short and confirm what happened after a
            tool call succeeds, or explain clearly if it failed.
            """;
}