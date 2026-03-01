package io.github.wendlmax.reaktor.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.net.URI;

public record ReaktorResponse<T>(
        ResponseEntity<T> entity,
        boolean success,
        String message,
        List<Error> errors,
        String view,
        java.util.Map<String, Object> context) {
    public record Error(String code, String field, String message) {
    }

    public ResponseEntity<T> toResponseEntity() {
        return entity;
    }

    public static <T> ReaktorResponse<T> ok(T data) {
        return new ReaktorResponse<>(ResponseEntity.ok(data), true, null, null, null, java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> ok() {
        return ok((T) null);
    }

    public static <T> ReaktorResponse<T> ok(java.util.Optional<T> optional) {
        return optional.map(ReaktorResponse::ok).orElseGet(() -> ReaktorResponse.notFound());
    }

    public static <T> ReaktorResponse<T> view(String viewName) {
        return new ReaktorResponse<>(ResponseEntity.ok().build(), true, null, null, viewName,
                java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> view(String viewName, T data) {
        return new ReaktorResponse<>(ResponseEntity.ok(data), true, null, null, viewName,
                java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> error(String message) {
        return new ReaktorResponse<>(ResponseEntity.badRequest().build(), false, message, null, null,
                java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> error(List<Error> errors) {
        return new ReaktorResponse<>(ResponseEntity.badRequest().build(), false, null, errors, null,
                java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> created(URI location, T data) {
        return new ReaktorResponse<>(ResponseEntity.created(location).body(data), true, null, null, null,
                java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> notFound() {
        return new ReaktorResponse<>(ResponseEntity.notFound().build(), false, "Resource not found", null, null,
                java.util.Collections.emptyMap());
    }

    public static <T> ReaktorResponse<T> redirect(String url) {
        return new ReaktorResponse<>(ResponseEntity.status(HttpStatus.FOUND).build(), true, null, null,
                "redirect:" + url, java.util.Collections.emptyMap());
    }

    public ReaktorResponse<T> withData(T data) {
        ResponseEntity<T> newEntity = ResponseEntity.status(entity.getStatusCode()).headers(entity.getHeaders())
                .body(data);
        return new ReaktorResponse<>(newEntity, success, message, errors, view, context);
    }

    public ReaktorResponse<T> withErrors(org.springframework.validation.Errors springErrors) {
        if (springErrors == null || !springErrors.hasErrors()) {
            return this;
        }

        java.util.List<Error> newErrors = new java.util.ArrayList<>();
        if (this.errors != null) {
            newErrors.addAll(this.errors);
        }

        springErrors.getFieldErrors()
                .forEach(e -> newErrors.add(new Error(e.getCode(), e.getField(), e.getDefaultMessage())));
        springErrors.getGlobalErrors().forEach(e -> newErrors.add(new Error(e.getCode(), null, e.getDefaultMessage())));

        ResponseEntity<T> badRequestEntity = ResponseEntity.badRequest().headers(entity.getHeaders())
                .body(entity.getBody());
        return new ReaktorResponse<>(badRequestEntity, false, message, newErrors, view, context);
    }

    public ReaktorResponse<T> withView(String viewName) {
        return new ReaktorResponse<>(entity, success, message, errors, viewName, context);
    }

    public ReaktorResponse<T> withRedirect(String url) {
        ResponseEntity<T> redirectEntity = ResponseEntity.status(HttpStatus.FOUND).headers(entity.getHeaders())
                .body(entity.getBody());
        return new ReaktorResponse<>(redirectEntity, success, message, errors, "redirect:" + url, context);
    }

    @SuppressWarnings("unchecked")
    public ReaktorResponse<Object> with(String name, Object value) {
        java.util.Map<String, Object> newData = extractBodyAsMap();
        newData.put(name, value);
        return new ReaktorResponse<>(shallowCopyEntity(newData), success, message, errors, view, context);
    }

    @SuppressWarnings("unchecked")
    public ReaktorResponse<Object> withAll(java.util.Map<String, ?> attributes) {
        java.util.Map<String, Object> newData = extractBodyAsMap();
        newData.putAll(attributes);
        return new ReaktorResponse<>(shallowCopyEntity(newData), success, message, errors, view, context);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> extractBodyAsMap() {
        if (entity.getBody() instanceof java.util.Map) {
            return new java.util.HashMap<>((java.util.Map<String, Object>) entity.getBody());
        }
        java.util.Map<String, Object> newData = new java.util.HashMap<>();
        if (entity.getBody() != null) {
            newData.put("data", entity.getBody());
        }
        return newData;
    }

    private <R> ResponseEntity<R> shallowCopyEntity(R newBody) {
        return ResponseEntity.status(entity.getStatusCode()).headers(entity.getHeaders()).body(newBody);
    }

    public ReaktorResponse<T> withContext(String name, Object value) {
        java.util.Map<String, Object> newContext = new java.util.HashMap<>(context);
        newContext.put(name, value);
        return new ReaktorResponse<>(entity, success, message, errors, view, newContext);
    }

    public ReaktorResponse<T> withStatus(HttpStatus status) {
        ResponseEntity<T> newEntity = ResponseEntity.status(status).headers(entity.getHeaders()).body(entity.getBody());
        return new ReaktorResponse<>(newEntity, success, message, errors, view, context);
    }

    // JSON ignore these metadata fields when serializing to client if needed
    // But for now, let's keep it simple and see how it looks.
}
