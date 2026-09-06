package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
 * Same agentic booking assistant as AssistantService, but backed by
 * Google's Gemini API (free tier) instead of Claude. Now also covers the
 * full catalog (City/Movie/Theatre/Screen/Show), not just booking/user.
 *
 * Uses plain REST calls against Gemini's documented generateContent
 * endpoint rather than the official SDK, since Google's Java SDK is
 * mid-migration to a new "Interactions API" as of 2026 and its exact
 * method surface is a moving target.
 */
@Service
@Slf4j
public class GeminiAssistantService {

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

    @Value("${GEMINI_API_KEY:${gemini.api.key:}}")
    private String apiKey;

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant for a movie ticket booking system. "
                    + "You can manage the full catalog (cities, theatres, screens, "
                    + "movies, shows) as well as user accounts and bookings, using the "
                    + "available tools. A screen's seats are generated when it is "
                    + "created, from the seatRows you provide. A show's bookable seats "
                    + "are generated when the show is created, from the screen it runs "
                    + "on, and its ticket prices come from the seatPrices you provide at "
                    + "that point. Ask for any missing required information before "
                    + "calling a tool, rather than guessing values (e.g. don't invent a "
                    + "cityId, screenId, or price). Keep replies short and confirm what "
                    + "happened after a tool call succeeds, or explain clearly if it failed.";

    public String handleMessage(String userMessage) {

        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(userTextContent(userMessage));

        for (int turn = 0; turn < 5; turn++) {

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.set("contents", contents);
            requestBody.set("system_instruction", textPart(SYSTEM_PROMPT));
            requestBody.set("tools", buildToolsNode());

            JsonNode response = callGemini(requestBody);

            JsonNode candidate = response.path("candidates").path(0);
            JsonNode modelContent = candidate.path("content");

            // Echo the model's own turn back into the running conversation.
            contents.add(modelContent);

            JsonNode parts = modelContent.path("parts");
            List<JsonNode> functionCalls = new ArrayList<>();
            StringBuilder textReply = new StringBuilder();

            for (JsonNode part : parts) {
                if (part.has("text")) {
                    textReply.append(part.get("text").asText());
                }
                if (part.has("functionCall")) {
                    functionCalls.add(part.get("functionCall"));
                }
            }

            if (functionCalls.isEmpty()) {
                return textReply.toString();
            }

            ObjectNode functionResponseContent = objectMapper.createObjectNode();
            functionResponseContent.put("role", "user");
            ArrayNode responseParts = objectMapper.createArrayNode();

            for (JsonNode call : functionCalls) {
                String name = call.get("name").asText();
                JsonNode args = call.get("args");
                String result = executeTool(name, args);

                ObjectNode functionResponsePart = objectMapper.createObjectNode();
                ObjectNode functionResponse = objectMapper.createObjectNode();
                functionResponse.put("name", name);
                ObjectNode responseWrapper = objectMapper.createObjectNode();
                responseWrapper.put("result", result);
                functionResponse.set("response", responseWrapper);
                functionResponsePart.set("functionResponse", functionResponse);
                responseParts.add(functionResponsePart);
            }

            functionResponseContent.set("parts", responseParts);
            contents.add(functionResponseContent);
        }

        return "Sorry, I couldn't complete that request in a reasonable number of steps.";
    }

    private JsonNode callGemini(ObjectNode requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            String url = ENDPOINT + "?key=" + apiKey;

            String rawResponse = restTemplate.postForObject(url, entity, String.class);
            return objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    private ObjectNode userTextContent(String text) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = objectMapper.createArrayNode();
        parts.add(textPart(text).get("parts").get(0));
        content.set("parts", parts);
        return content;
    }

    private ObjectNode textPart(String text) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", text);
        parts.add(part);
        wrapper.set("parts", parts);
        return wrapper;
    }

    // ---- Tool execution ----

    private String executeTool(String toolName, JsonNode args) {
        try {
            switch (toolName) {

                // ---- Booking / User ----

                case "create_booking": {
                    Long showId = args.get("showId").asLong();
                    Long userId = args.get("userId").asLong();
                    List<String> seatNumbers = new ArrayList<>();
                    args.get("seatNumbers").forEach(n -> seatNumbers.add(n.asText()));

                    Booking booking = bookingServiceImpl.createBooking(seatNumbers, showId, userId);
                    return String.format(
                            "Booking created. id=%d, status=%s, totalAmount=%.2f",
                            booking.getId(), booking.getBookingStatus(), booking.getTotalAmount());
                }

                case "get_ticket": {
                    Long bookingId = args.get("bookingId").asLong();
                    Booking booking = bookingServiceImpl.getBookingById(bookingId);
                    return String.format(
                            "Ticket id=%d, status=%s, totalAmount=%.2f, bookedBy=%s",
                            booking.getId(), booking.getBookingStatus(),
                            booking.getTotalAmount(), booking.getCreatedBy().getEmail());
                }

                case "register_user": {
                    String email = args.get("email").asText();
                    String password = args.get("password").asText();
                    User user = userServiceImpl.registerUser(email, password);
                    return String.format("User registered. id=%d, email=%s", user.getId(), user.getEmail());
                }

                case "login_user": {
                    String email = args.get("email").asText();
                    String password = args.get("password").asText();
                    boolean success = userServiceImpl.login(email, password);
                    return success ? "Login successful." : "Login failed.";
                }

                // ---- City ----

                case "create_city": {
                    String name = args.get("name").asText();
                    City city = cityServiceImpl.createCity(name);
                    return String.format("City created. id=%d, name=%s", city.getId(), city.getName());
                }

                // ---- Movie ----

                case "create_movie": {
                    String name = args.get("name").asText();
                    String rating = args.has("rating") ? args.get("rating").asText() : null;
                    Long duration = args.get("duration").asLong();

                    List<Feature> features = args.has("features")
                            ? Arrays.asList(objectMapper.convertValue(args.get("features"), Feature[].class))
                            : List.of();

                    List<String> languages = args.has("languages")
                            ? Arrays.asList(objectMapper.convertValue(args.get("languages"), String[].class))
                            : List.of();

                    List<Long> actorIds = args.has("actorIds")
                            ? Arrays.asList(objectMapper.convertValue(args.get("actorIds"), Long[].class))
                            : null;

                    Movie movie = movieServiceImpl.createMovie(name, rating, features, languages, duration, actorIds);
                    return String.format("Movie created. id=%d, name=%s", movie.getId(), movie.getName());
                }

                case "get_movie": {
                    Long movieId = args.get("movieId").asLong();
                    Movie movie = movieServiceImpl.getMovieById(movieId);
                    return String.format(
                            "Movie id=%d, name=%s, rating=%s, durationMinutes=%d",
                            movie.getId(), movie.getName(), movie.getRating(), movie.getDuration());
                }

                // ---- Theatre ----

                case "create_theatre": {
                    String name = args.get("name").asText();
                    Long cityId = args.get("cityId").asLong();
                    Theatre theatre = theatreServiceImpl.createTheatre(name, cityId);
                    return String.format("Theatre created. id=%d, name=%s", theatre.getId(), theatre.getName());
                }

                case "get_theatre": {
                    Long theatreId = args.get("theatreId").asLong();
                    Theatre theatre = theatreServiceImpl.getTheatreById(theatreId);
                    return String.format(
                            "Theatre id=%d, name=%s, city=%s",
                            theatre.getId(), theatre.getName(),
                            theatre.getCity() != null ? theatre.getCity().getName() : "unknown");
                }

                // ---- Screen ----

                case "create_screen": {
                    CreateScreenRequestDTO requestDTO = objectMapper.treeToValue(args, CreateScreenRequestDTO.class);
                    Screen screen = screenServiceImpl.createScreen(requestDTO);
                    return String.format(
                            "Screen created. id=%d, name=%s, totalSeats=%d",
                            screen.getId(), screen.getName(),
                            screen.getSeats() == null ? 0 : screen.getSeats().size());
                }

                case "get_screen": {
                    Long screenId = args.get("screenId").asLong();
                    Screen screen = screenServiceImpl.getScreenById(screenId);
                    return String.format(
                            "Screen id=%d, name=%s, totalSeats=%d",
                            screen.getId(), screen.getName(),
                            screen.getSeats() == null ? 0 : screen.getSeats().size());
                }

                // ---- Show ----

                case "create_show": {
                    CreateShowRequestDTO requestDTO = objectMapper.treeToValue(args, CreateShowRequestDTO.class);
                    Show show = showServiceImpl.createShow(requestDTO);
                    int totalSeats = showServiceImpl.getShowSeats(show).size();
                    return String.format(
                            "Show created. id=%d, movie=%s, totalSeats=%d",
                            show.getId(),
                            show.getMovie() != null ? show.getMovie().getName() : "unknown",
                            totalSeats);
                }

                case "get_show": {
                    Long showId = args.get("showId").asLong();
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
            log.warn("Tool execution failed: {}", toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    // ---- Tool schemas, in Gemini's functionDeclarations format ----

    private ArrayNode buildToolsNode() {
        ArrayNode tools = objectMapper.createArrayNode();
        ObjectNode toolEntry = objectMapper.createObjectNode();
        ArrayNode declarations = objectMapper.createArrayNode();

        // ---- Booking / User ----

        declarations.add(functionDeclaration(
                "create_booking",
                "Book seats for a show. Requires an existing showId and userId.",
                Map.of(
                        "showId", Map.of("type", "INTEGER"),
                        "userId", Map.of("type", "INTEGER"),
                        "seatNumbers", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                ),
                List.of("showId", "userId", "seatNumbers")
        ));

        declarations.add(functionDeclaration(
                "get_ticket",
                "Look up an existing booking/ticket by its ID.",
                Map.of("bookingId", Map.of("type", "INTEGER")),
                List.of("bookingId")
        ));

        declarations.add(functionDeclaration(
                "register_user",
                "Register a new user account with an email and password.",
                Map.of(
                        "email", Map.of("type", "STRING"),
                        "password", Map.of("type", "STRING")
                ),
                List.of("email", "password")
        ));

        declarations.add(functionDeclaration(
                "login_user",
                "Log in an existing user with email and password.",
                Map.of(
                        "email", Map.of("type", "STRING"),
                        "password", Map.of("type", "STRING")
                ),
                List.of("email", "password")
        ));

        // ---- City ----

        declarations.add(functionDeclaration(
                "create_city",
                "Create a new city that theatres can belong to.",
                Map.of("name", Map.of("type", "STRING")),
                List.of("name")
        ));

        // ---- Movie ----

        declarations.add(functionDeclaration(
                "create_movie",
                "Create a new movie. actorIds is optional and references existing actors.",
                Map.of(
                        "name", Map.of("type", "STRING"),
                        "rating", Map.of("type", "STRING"),
                        "features", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING", "enum", List.of("DOLBY_ATMOS", "FOUR_D", "FIVE_D", "IMAX"))
                        ),
                        "languages", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "duration", Map.of("type", "INTEGER", "description", "Duration in minutes."),
                        "actorIds", Map.of("type", "ARRAY", "items", Map.of("type", "INTEGER"))
                ),
                List.of("name", "duration")
        ));

        declarations.add(functionDeclaration(
                "get_movie",
                "Look up an existing movie by its ID.",
                Map.of("movieId", Map.of("type", "INTEGER")),
                List.of("movieId")
        ));

        // ---- Theatre ----

        declarations.add(functionDeclaration(
                "create_theatre",
                "Create a new theatre in an existing city.",
                Map.of(
                        "name", Map.of("type", "STRING"),
                        "cityId", Map.of("type", "INTEGER")
                ),
                List.of("name", "cityId")
        ));

        declarations.add(functionDeclaration(
                "get_theatre",
                "Look up an existing theatre by its ID.",
                Map.of("theatreId", Map.of("type", "INTEGER")),
                List.of("theatreId")
        ));

        // ---- Screen ----

        declarations.add(functionDeclaration(
                "create_screen",
                "Create a new screen inside an existing theatre, generating its seats "
                        + "from a list of seat rows. Each row produces seats numbered like "
                        + "A1, A2, ... based on rowLabel and numberOfSeats.",
                Map.of(
                        "theatreId", Map.of("type", "INTEGER"),
                        "name", Map.of("type", "STRING"),
                        "features", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING", "enum", List.of("DOLBY_ATMOS", "FOUR_D", "FIVE_D", "IMAX"))
                        ),
                        "seatRows", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "rowLabel", Map.of("type", "STRING"),
                                                "numberOfSeats", Map.of("type", "INTEGER"),
                                                "seatType", Map.of("type", "STRING", "enum", List.of("RECLINER", "GOLD", "SILVER"))
                                        ),
                                        "required", List.of("rowLabel", "numberOfSeats", "seatType")
                                )
                        )
                ),
                List.of("theatreId", "name", "seatRows")
        ));

        declarations.add(functionDeclaration(
                "get_screen",
                "Look up an existing screen by its ID.",
                Map.of("screenId", Map.of("type", "INTEGER")),
                List.of("screenId")
        ));

        // ---- Show ----

        declarations.add(functionDeclaration(
                "create_show",
                "Schedule a new show for a movie on a screen at a start time, with "
                        + "per-seat-type pricing. This generates the bookable seats for the show.",
                Map.of(
                        "movieId", Map.of("type", "INTEGER"),
                        "screenId", Map.of("type", "INTEGER"),
                        "startTime", Map.of("type", "STRING", "description", "ISO-8601 date-time, e.g. 2026-09-10T18:30:00.000Z"),
                        "features", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING", "enum", List.of("DOLBY_ATMOS", "FOUR_D", "FIVE_D", "IMAX"))
                        ),
                        "seatPrices", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "seatType", Map.of("type", "STRING", "enum", List.of("RECLINER", "GOLD", "SILVER")),
                                                "price", Map.of("type", "NUMBER")
                                        ),
                                        "required", List.of("seatType", "price")
                                )
                        )
                ),
                List.of("movieId", "screenId", "startTime", "seatPrices")
        ));

        declarations.add(functionDeclaration(
                "get_show",
                "Look up an existing show by its ID.",
                Map.of("showId", Map.of("type", "INTEGER")),
                List.of("showId")
        ));

        toolEntry.set("functionDeclarations", declarations);
        tools.add(toolEntry);
        return tools;
    }

    private ObjectNode functionDeclaration(String name, String description,
                                           Map<String, Object> properties,
                                           List<String> required) {
        ObjectNode decl = objectMapper.createObjectNode();
        decl.put("name", name);
        decl.put("description", description);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "OBJECT");
        parameters.set("properties", objectMapper.valueToTree(properties));

        ArrayNode requiredArray = objectMapper.createArrayNode();
        required.forEach(requiredArray::add);
        parameters.set("required", requiredArray);

        decl.set("parameters", parameters);
        return decl;
    }
}