package io.github.wendlmax.reaktor.web;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReaktorGlobalExceptionHandlerTest {

    private final ReaktorGlobalExceptionHandler handler = new ReaktorGlobalExceptionHandler();

    @Test
    void handleValidationExceptions() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "age", "Age must be greater than 18"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ReaktorResponse<Object> response = handler.handleValidationExceptions(exception);

        assertThat(response.success()).isFalse();
        assertThat(response.errors()).hasSize(1);
        ReaktorResponse.Error err = response.errors().get(0);
        assertThat(err.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(err.field()).isEqualTo("age");
        assertThat(err.message()).isEqualTo("Age must be greater than 18");
    }

    @Test
    void handleAllExceptions() {
        Exception exception = new RuntimeException("Unexpected fatal error");

        ReaktorResponse<Object> response = handler.handleAllExceptions(exception);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Unexpected fatal error");
    }
}
