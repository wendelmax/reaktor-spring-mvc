package io.github.wendlmax.reaktor.fragment;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReaktorFragmentsTest {

    @Test
    void shouldBuildFragments() {
        List<ModelAndView> fragments = ReaktorFragments.fragment("my-view")
                .fragment("other-view", "data", "value")
                .fragment("map-view", Map.of("k1", "v1"))
                .fragment("null-map-view", (Map<String, ?>) null)
                .build();

        assertThat(fragments).hasSize(4);

        assertThat(fragments.get(0).getViewName()).isEqualTo("my-view");
        assertThat(fragments.get(0).getModel()).isEmpty();

        assertThat(fragments.get(1).getViewName()).isEqualTo("other-view");
        assertThat(fragments.get(1).getModel()).containsEntry("data", "value");

        assertThat(fragments.get(2).getViewName()).isEqualTo("map-view");
        assertThat(fragments.get(2).getModel()).containsEntry("k1", "v1");

        assertThat(fragments.get(3).getViewName()).isEqualTo("null-map-view");
        assertThat(fragments.get(3).getModel()).isEmpty();
    }
}
