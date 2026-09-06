package com.example.scaler.bms.may2026.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.services.blocking.MessageService;

import com.example.scaler.bms.may2026.model.City;

/**
 * Tests AssistantService's tool-calling logic without ever hitting the
 * real Anthropic API: AnthropicClient, MessageService, and the response
 * types (Message/ContentBlock/ToolUseBlock/TextBlock) are all mocked, so
 * this costs nothing and needs no ANTHROPIC_API_KEY to run.
 */
class AssistantServiceTest {

    private AnthropicClient anthropicClient;
    private MessageService messageService;

    private CityServiceImpl cityServiceImpl;
    private BookingServiceImpl bookingServiceImpl;
    private UserServiceImpl userServiceImpl;
    private MovieServiceImpl movieServiceImpl;
    private TheatreServiceImpl theatreServiceImpl;
    private ScreenServiceImpl screenServiceImpl;
    private ShowServiceImpl showServiceImpl;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {

        anthropicClient = mock(AnthropicClient.class);
        messageService = mock(MessageService.class);
        when(anthropicClient.messages()).thenReturn(messageService);

        cityServiceImpl = mock(CityServiceImpl.class);
        bookingServiceImpl = mock(BookingServiceImpl.class);
        userServiceImpl = mock(UserServiceImpl.class);
        movieServiceImpl = mock(MovieServiceImpl.class);
        theatreServiceImpl = mock(TheatreServiceImpl.class);
        screenServiceImpl = mock(ScreenServiceImpl.class);
        showServiceImpl = mock(ShowServiceImpl.class);

        // Package-private constructor: injects the mock client instead of
        // a real one built from ANTHROPIC_API_KEY.
        assistantService = new AssistantService(anthropicClient);

        ReflectionTestUtils.setField(assistantService, "cityServiceImpl", cityServiceImpl);
        ReflectionTestUtils.setField(assistantService, "bookingServiceImpl", bookingServiceImpl);
        ReflectionTestUtils.setField(assistantService, "userServiceImpl", userServiceImpl);
        ReflectionTestUtils.setField(assistantService, "movieServiceImpl", movieServiceImpl);
        ReflectionTestUtils.setField(assistantService, "theatreServiceImpl", theatreServiceImpl);
        ReflectionTestUtils.setField(assistantService, "screenServiceImpl", screenServiceImpl);
        ReflectionTestUtils.setField(assistantService, "showServiceImpl", showServiceImpl);
    }

    @Test
    void handleMessage_callsCreateCityTool_andReturnsFinalReply() throws Exception {

        // ---- Arrange: what the real service should do when the tool fires ----

        City savedCity = new City();
        savedCity.setId(42L);
        savedCity.setName("Chennai");
        when(cityServiceImpl.createCity("Chennai")).thenReturn(savedCity);

        // ---- Arrange: Claude's first "turn" -- it wants to call create_city ----

        ToolUseBlock toolUseBlock = mock(ToolUseBlock.class);
        when(toolUseBlock.id()).thenReturn("toolu_01");
        when(toolUseBlock.name()).thenReturn("create_city");
        when(toolUseBlock._input()).thenReturn(JsonValue.from(Map.of("name", "Chennai")));

        ContentBlock toolUseContentBlock = mock(ContentBlock.class);
        when(toolUseContentBlock.text()).thenReturn(Optional.empty());
        when(toolUseContentBlock.toolUse()).thenReturn(Optional.of(toolUseBlock));

        Message toolCallResponse = mock(Message.class);
        when(toolCallResponse.content()).thenReturn(List.of(toolUseContentBlock));
        when(toolCallResponse.toParam()).thenReturn(
                MessageParam.builder().role(MessageParam.Role.ASSISTANT).content("(tool call)").build());

        // ---- Arrange: Claude's second "turn" -- final text answer, no more tools ----

        TextBlock textBlock = mock(TextBlock.class);
        when(textBlock.text()).thenReturn("City 'Chennai' has been created.");

        ContentBlock finalContentBlock = mock(ContentBlock.class);
        when(finalContentBlock.text()).thenReturn(Optional.of(textBlock));
        when(finalContentBlock.toolUse()).thenReturn(Optional.empty());

        Message finalResponse = mock(Message.class);
        when(finalResponse.content()).thenReturn(List.of(finalContentBlock));
        when(finalResponse.toParam()).thenReturn(
                MessageParam.builder().role(MessageParam.Role.ASSISTANT)
                        .content("City 'Chennai' has been created.").build());

        // First call to create(...) returns the tool-call turn, second call
        // returns the final answer -- mirroring the real two-round-trip flow.
        when(messageService.create(any(MessageCreateParams.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);

        // ---- Act ----

        String reply = assistantService.handleMessage("Create a city called Chennai");

        // ---- Assert ----

        assertEquals("City 'Chennai' has been created.", reply);
        verify(cityServiceImpl).createCity("Chennai");
    }

    @Test
    void handleMessage_whenNoToolCallNeeded_returnsTextDirectly() throws Exception {

        // Claude answers immediately with no tool use at all.
        TextBlock textBlock = mock(TextBlock.class);
        when(textBlock.text()).thenReturn("I can help you book movie tickets -- what would you like to do?");

        ContentBlock contentBlock = mock(ContentBlock.class);
        when(contentBlock.text()).thenReturn(Optional.of(textBlock));
        when(contentBlock.toolUse()).thenReturn(Optional.empty());

        Message response = mock(Message.class);
        when(response.content()).thenReturn(List.of(contentBlock));
        when(response.toParam()).thenReturn(
                MessageParam.builder().role(MessageParam.Role.ASSISTANT)
                        .content("I can help you book movie tickets -- what would you like to do?").build());

        when(messageService.create(any(MessageCreateParams.class))).thenReturn(response);

        String reply = assistantService.handleMessage("Hi, what can you do?");

        assertEquals("I can help you book movie tickets -- what would you like to do?", reply);

        // No catalog/booking/user service should have been touched.
        org.mockito.Mockito.verifyNoInteractions(
                cityServiceImpl, bookingServiceImpl, userServiceImpl,
                movieServiceImpl, theatreServiceImpl, screenServiceImpl, showServiceImpl);
    }
}
