package com.arthasmanager.model;

import com.arthasmanager.model.vo.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void success_withData_returns200AndData() {
        Result<String> result = Result.success("hello");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    void success_withNull_returns200AndNullData() {
        Result<Void> result = Result.success(null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    void success_noArg_returns200AndNullData() {
        Result<Void> result = Result.success();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isNull();
    }

    @Test
    void error_withMessage_returns500() {
        Result<Object> result = Result.error("something went wrong");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("something went wrong");
        assertThat(result.getData()).isNull();
    }

    @Test
    void error_withCodeAndMessage_returnsCustomCode() {
        Result<Object> result = Result.error(404, "not found");

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).isEqualTo("not found");
        assertThat(result.getData()).isNull();
    }

    @Test
    void success_withList_wrapsListCorrectly() {
        java.util.List<String> list = java.util.List.of("a", "b", "c");
        Result<java.util.List<String>> result = Result.success(list);

        assertThat(result.getData()).hasSize(3).containsExactly("a", "b", "c");
    }
}
