package com.firesafety.platform.training;

import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.file.FileStorage;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrainingConfiguration {
    @Bean
    TrainingScorer trainingScorer() { return new TrainingScorer(); }

    @Bean
    TrainingQuestionExcelParser trainingQuestionExcelParser() { return new TrainingQuestionExcelParser(); }

    @Bean
    TrainingManagementService trainingManagementService(
            TrainingQuestionRepository questions,
            TrainingTaskRepository tasks,
            TrainingParticipantRepository participants,
            UserAccountRepository users,
            TrainingNotificationPort notifications,
            Clock clock) {
        return new TrainingManagementService(questions, tasks, participants, users, notifications, clock);
    }

    @Bean
    TrainingAttemptService trainingAttemptService(
            TrainingParticipantRepository participants,
            TrainingTaskRepository tasks,
            TrainingQuestionRepository questions,
            TrainingRecordRepository records,
            TrainingAnswerDetailRepository answerDetails,
            TrainingScorer scorer,
            TrainingCertificatePort certificates,
            Clock clock) {
        return new TrainingAttemptService(
                participants, tasks, questions, records, answerDetails, scorer, certificates, clock);
    }

    @Bean
    TrainingRecordQueryService trainingRecordQueryService(
            TrainingRecordRepository records,
            TrainingTaskRepository tasks,
            UserAccountRepository users,
            EnterpriseRepository enterprises) {
        return new TrainingRecordQueryService(records, tasks, users, enterprises);
    }

    @Bean
    CertificateRenderer certificateRenderer() { return new OpenPdfCertificateRenderer(); }

    @Bean
    TrainingCertificateService trainingCertificateService(
            TrainingCertificateRepository certificates,
            EnterpriseRepository enterprises,
            FileStorage storage,
            CertificateRenderer renderer,
            @Value("${app.certificate.number-prefix:FS}") String numberPrefix,
            @Value("${app.certificate.issuer-name:企业消防安全培训平台}") String issuerName) {
        return new TrainingCertificateService(
                certificates, enterprises, storage, renderer, numberPrefix, issuerName);
    }
}
