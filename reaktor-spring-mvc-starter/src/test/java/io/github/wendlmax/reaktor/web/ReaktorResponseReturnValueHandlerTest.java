package io.github.wendlmax.reaktor.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReaktorResponseReturnValueHandlerTest {

    private ReaktorResponseReturnValueHandler handler;
    private ModelAndViewContainer mavContainer;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new ReaktorResponseReturnValueHandler(new ObjectMapper());
        mavContainer = new ModelAndViewContainer();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        webRequest = new ServletWebRequest(request, response);
    }

    // Dummy controllers for MethodParameter reflection
    static class RestTestController {
        @ResponseBody
        public ReaktorResponse<String> respondJson() {
            return ReaktorResponse.ok("json-data");
        }
    }

    static class ViewTestController {
        public ReaktorResponse<String> respondView() {
            return ReaktorResponse.ok("view-data");
        }
    }

    // Test the supportsReturnType
    @Test
    void supportsReturnType() throws Exception {
        Method jsonMethod = RestTestController.class.getMethod("respondJson");
        MethodParameter returnType = new MethodParameter(jsonMethod, -1);
        assertThat(handler.supportsReturnType(returnType)).isTrue();
    }

    // Test ResponseBody handling (write JSON)
    @Test
    void handleReturnValueJson() throws Exception {
        Method jsonMethod = RestTestController.class.getMethod("respondJson");
        MethodParameter returnType = new MethodParameter(jsonMethod, -1);

        ReaktorResponse<String> responseObj = ReaktorResponse.ok("data");
        handler.handleReturnValue(responseObj, returnType, mavContainer, webRequest);

        assertThat(mavContainer.isRequestHandled()).isTrue();
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"success\":true");
        assertThat(response.getContentAsString()).contains("\"body\":\"data\""); // Spring ResponseEntity serializes
                                                                                 // body
    }

    @Test
    void handleNullReturnValue() throws Exception {
        Method jsonMethod = RestTestController.class.getMethod("respondJson");
        MethodParameter returnType = new MethodParameter(jsonMethod, -1);
        handler.handleReturnValue(null, returnType, mavContainer, webRequest);
        assertThat(mavContainer.isRequestHandled()).isFalse();
    }

    // Test standard View handling
    @Test
    void handleReturnValueView() throws Exception {
        Method viewMethod = ViewTestController.class.getMethod("respondView");
        MethodParameter returnType = new MethodParameter(viewMethod, -1);

        ReaktorResponse<String> responseObj = ReaktorResponse.ok("view-data")
                .withView("custom/view")
                .withContext("ctxKey", "ctxVal")
                .withErrors(null); // Just for coverage

        handler.handleReturnValue(responseObj, returnType, mavContainer, webRequest);

        assertThat(mavContainer.isRequestHandled()).isFalse();
        assertThat(mavContainer.getViewName()).isEqualTo("custom/view");
        assertThat(mavContainer.getModel().get("data")).isEqualTo("view-data");
        assertThat(mavContainer.getModel().get("success")).isEqualTo(true);
        assertThat(mavContainer.getModel().get("__reaktor_context")).isInstanceOf(Map.class);
        Map<?, ?> contextMap = (Map<?, ?>) mavContainer.getModel().get("__reaktor_context");
        assertThat(contextMap.get("ctxKey")).isEqualTo("ctxVal");
        assertThat(mavContainer.getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void handleReturnValueViewFallbackName() throws Exception {
        Method viewMethod = ViewTestController.class.getMethod("respondView");
        MethodParameter returnType = new MethodParameter(viewMethod, -1);

        ReaktorResponse<String> responseObj = ReaktorResponse.<String>error("Error occurred")
                .withData("dummy-data");

        handler.handleReturnValue(responseObj, returnType, mavContainer, webRequest);

        assertThat(mavContainer.getViewName()).isEqualTo("viewtest/respondView");
        assertThat(mavContainer.getModel().get("message")).isEqualTo("Error occurred");
        assertThat(mavContainer.getModel().get("success")).isEqualTo(false);
        assertThat(mavContainer.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleReturnValueViewWithMapData() throws Exception {
        Method viewMethod = ViewTestController.class.getMethod("respondView");
        MethodParameter returnType = new MethodParameter(viewMethod, -1);

        ReaktorResponse<Map<String, String>> responseObj = ReaktorResponse.ok(Map.of("prop1", "val1"));

        handler.handleReturnValue(responseObj, returnType, mavContainer, webRequest);

        assertThat(mavContainer.getModel().get("prop1")).isEqualTo("val1");
    }

    @Test
    void testConstructors() {
        ReaktorResponseReturnValueHandler handler1 = new ReaktorResponseReturnValueHandler((List) null);
        ReaktorResponseReturnValueHandler handler2 = new ReaktorResponseReturnValueHandler(new ObjectMapper());
        assertThat(handler1).isNotNull();
        assertThat(handler2).isNotNull();
    }
}
