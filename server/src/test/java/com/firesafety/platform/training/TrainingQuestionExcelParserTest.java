package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class TrainingQuestionExcelParserTest {
    private static final List<String> HEADERS = List.of(
            "题型", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "分值", "分类", "解析");

    private final TrainingQuestionExcelParser parser = new TrainingQuestionExcelParser();

    @Test
    void parsesSupportedQuestionTypesAndEvaluatesFormulaCells() throws Exception {
        byte[] workbook = workbook(sheet -> {
            row(sheet, 1, "单选", "灭火器使用前首先做什么？", "拔掉保险销", "按下电梯按钮", "", "", "A", "1000", "消防常识", "先检查并拔销");
            row(sheet, 2, "多选", "以下属于火灾逃生正确做法的是？", "低姿前进", "返回取财物", "沿疏散指示撤离", "乘坐电梯", "A,C", "35", "疏散逃生", "多选必须完全匹配");
            row(sheet, 3, "判断", "发生火灾时可以乘坐电梯。", "", "", "", "", "错误", null, "疏散逃生", "应走安全出口");
            sheet.getRow(3).createCell(7, CellType.FORMULA).setCellFormula("20+5");
        });

        var result = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(result.errors()).isEmpty();
        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions().get(0).type()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(result.questions().get(0).score()).isEqualTo(1000);
        assertThat(result.questions().get(1).correctAnswers()).isEqualTo(Set.of("A", "C"));
        assertThat(result.questions().get(2).type()).isEqualTo(QuestionType.TRUE_FALSE);
        assertThat(result.questions().get(2).options()).containsEntry("TRUE", "正确").containsEntry("FALSE", "错误");
        assertThat(result.questions().get(2).correctAnswers()).isEqualTo(Set.of("FALSE"));
        assertThat(result.questions().get(2).score()).isEqualTo(25);
    }

    @Test
    void reportsEveryInvalidRowWithoutReturningPartialQuestions() throws Exception {
        byte[] workbook = workbook(sheet -> {
            row(sheet, 1, "单选", "缺少正确答案", "选项一", "选项二", "", "", "", "10", "消防常识", "");
            row(sheet, 2, "未知题型", "错误题型", "选项一", "选项二", "", "", "A", "10", "消防常识", "");
        });

        var result = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.questions()).isEmpty();
        assertThat(result.errors()).extracting(QuestionImportError::rowNumber).containsExactly(2, 3);
        assertThat(result.errors()).extracting(QuestionImportError::message)
                .anyMatch(message -> message.contains("正确答案"))
                .anyMatch(message -> message.contains("题型"));
    }

    @Test
    void createsImportTemplateWithRequiredHeadersAndExampleRow() throws Exception {
        byte[] template = parser.createTemplate();

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                assertThat(header.getCell(index).getStringCellValue()).isEqualTo(HEADERS.get(index));
            }
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("单选");
        }
    }

    private byte[] workbook(SheetWriter writer) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("题库导入");
            var header = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                header.createCell(index).setCellValue(HEADERS.get(index));
            }
            writer.write(sheet);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void row(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.length; index++) {
            if (values[index] != null) row.createCell(index).setCellValue(values[index]);
        }
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(org.apache.poi.ss.usermodel.Sheet sheet) throws Exception;
    }
}
