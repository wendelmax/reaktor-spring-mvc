package io.github.wendlmax.reaktor.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReaktorResponseTest {

    @Test
    void shouldCreateOkResponse() {
        ReaktorResponse<String> response = ReaktorResponse.ok("test-data");
        assertThat(response.success()).isTrue();
        assertThat(response.entity().getBody()).isEqualTo("test-data");
        assertThat(response.entity().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldCreateEmptyOkResponse() {
        ReaktorResponse<Object> response = ReaktorResponse.ok();
        assertThat(response.success()).isTrue();
        assertThat(response.entity().getBody()).isNull();
    }

    @Test
    void shouldCreateOkFromOptional() {
        ReaktorResponse<String> r1 = ReaktorResponse.ok(Optional.of("present"));
        assertThat(r1.success()).isTrue();
        assertThat(r1.entity().getBody()).isEqualTo("present");

        ReaktorResponse<String> r2 = ReaktorResponse.ok(Optional.empty());
        assertThat(r2.success()).isFalse();
        assertThat(r2.entity().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldCreateViewResponse() {
        ReaktorResponse<Object> r1 = ReaktorResponse.view("my-view");
        assertThat(r1.view()).isEqualTo("my-view");
        assertThat(r1.entity().getBody()).isNull();

        ReaktorResponse<String> r2 = ReaktorResponse.view("my-data-view", "some-data");
        assertThat(r2.view()).isEqualTo("my-data-view");
        assertThat(r2.entity().getBody()).isEqualTo("some-data");
    }

    @Test
    void shouldCreateErrorResponseWithMessage() {
        ReaktorResponse<Object> response = ReaktorResponse.error("something went wrong");
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("something went wrong");
        assertThat(response.entity().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCreateErrorResponseWithList() {
        ReaktorResponse.Error err = new ReaktorResponse.Error("E001", "name", "bad name");
        ReaktorResponse<Object> response = ReaktorResponse.error(List.of(err));
        assertThat(response.success()).isFalse();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).field()).isEqualTo("name");
    }

    @Test
    void shouldCreateCreatedResponse() {
        ReaktorResponse<String> response = ReaktorResponse.created(URI.create("/new-resource"), "data");
        assertThat(response.success()).isTrue();
        assertThat(response.entity().getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.entity().getHeaders().getLocation().toString()).isEqualTo("/new-resource");
    }

    @Test
    void shouldCreateRedirectResponse() {
        ReaktorResponse<Object> response = ReaktorResponse.redirect("/home");
        assertThat(response.success()).isTrue();
        assertThat(response.entity().getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.view()).isEqualTo("redirect:/home");
    }

    @Test
    void shouldMutateWithData() {
        ReaktorResponse<String> r = ReaktorResponse.ok("initial").withData("changed");
        assertThat(r.entity().getBody()).isEqualTo("changed");
    }

    @Test
    void shouldMutateWithView() {
        ReaktorResponse<String> r = ReaktorResponse.ok("data").withView("user-list");
        assertThat(r.view()).isEqualTo("user-list");
    }

    @Test
    void shouldMutateWithRedirect() {
        ReaktorResponse<String> r = ReaktorResponse.ok("data").withRedirect("/login");
        assertThat(r.entity().getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(r.view()).isEqualTo("redirect:/login");
    }

    @Test
    void shouldMutateWithContext() {
        ReaktorResponse<String> r = ReaktorResponse.ok("data").withContext("isAdmin", true);
        assertThat(r.context()).containsEntry("isAdmin", true);
    }

    @Test
    void shouldMutateWithStatus() {
        ReaktorResponse<String> r = ReaktorResponse.ok("data").withStatus(HttpStatus.ACCEPTED);
        assertThat(r.entity().getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void shouldMutateWithSpringErrors() {
        Errors springErrors = new BeanPropertyBindingResult(new Object(), "target");
        springErrors.rejectValue("age", "TooYoung", "Age is too low");
        springErrors.reject("GlobalErr", "General failure");

        ReaktorResponse<String> r = ReaktorResponse.ok("data").withErrors(springErrors);
        assertThat(r.success()).isFalse();
        assertThat(r.entity().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.errors()).hasSize(2);
        assertThat(r.errors().get(0).field()).isEqualTo("age");
        assertThat(r.errors().get(1).field()).isNull();

        // Return same object if no errors
        Errors emptyErrors = new BeanPropertyBindingResult(new Object(), "target");
        ReaktorResponse<String> r2 = r.withErrors(emptyErrors);
        assertThat(r2).isSameAs(r);
    }

    @Test
    void shouldMutateWithAndWithAllModifyingMap() {
        ReaktorResponse<Object> r1 = ReaktorResponse.ok().with("foo", "bar");
        assertThat((Map<String, Object>) r1.entity().getBody()).containsEntry("foo", "bar");

        ReaktorResponse<Object> r2 = r1.withAll(Map.of("hello", "world"));
        assertThat((Map<String, Object>) r2.entity().getBody())
                .containsEntry("foo", "bar")
                .containsEntry("hello", "world");

        // Test when body is not a map
        ReaktorResponse<String> r3 = ReaktorResponse.ok("simple-string");
        ReaktorResponse<Object> r3Map = r3.with("extra", "value");
        assertThat((Map<String, Object>) r3Map.entity().getBody())
                .containsEntry("data", "simple-string")
                .containsEntry("extra", "value");

        ReaktorResponse<Object> r3MapAll = r3.withAll(Map.of("more", "items"));
        assertThat((Map<String, Object>) r3MapAll.entity().getBody())
                .containsEntry("data", "simple-string")
                .containsEntry("more", "items");
    }
}
