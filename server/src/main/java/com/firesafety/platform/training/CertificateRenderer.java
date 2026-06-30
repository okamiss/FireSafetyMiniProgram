package com.firesafety.platform.training;

public interface CertificateRenderer {
    byte[] render(CertificateContent content);
}
