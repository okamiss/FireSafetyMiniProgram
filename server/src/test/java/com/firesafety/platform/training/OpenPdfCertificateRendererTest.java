package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OpenPdfCertificateRendererTest {
    @Test
    void rendersPdfCertificateWithEmbeddedChineseFont() {
        var renderer = new OpenPdfCertificateRenderer();

        var bytes = renderer.render(new CertificateContent(
                "FS-2026-000001", "张三", "示例消防企业", "2026 年度消防安全培训",
                LocalDate.of(2026, 6, 30), "企业消防安全培训平台"));

        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(bytes.length).isGreaterThan(10_000);
    }
}
