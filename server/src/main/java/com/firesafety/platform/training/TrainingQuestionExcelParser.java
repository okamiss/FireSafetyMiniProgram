package com.firesafety.platform.training;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TrainingQuestionExcelParser {
    private static final int MAX_ROWS = 1_000;
    private static final List<String> HEADERS = List.of(
            "题型", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "分值", "分类", "解析");

    public byte[] createTemplate() throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("题库导入");
            var header = sheet.createRow(0);
            var headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int index = 0; index < HEADERS.size(); index++) {
                var cell = header.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }
            var example = sheet.createRow(1);
            var values = List.of(
                    "单选", "灭火器使用前首先做什么？", "拔掉保险销", "乘坐电梯", "", "", "A", "10", "消防常识", "先检查并拔掉保险销");
            for (int index = 0; index < values.size(); index++) example.createCell(index).setCellValue(values.get(index));
            for (int index = 0; index < HEADERS.size(); index++) {
                sheet.autoSizeColumn(index);
                sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 800, 12_000));
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    public QuestionImportParseResult parse(InputStream input) throws IOException {
        try (var workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                return failure(1, "Excel 文件中没有工作表");
            }
            var sheet = workbook.getSheetAt(0);
            var formatter = new DataFormatter(Locale.CHINA, true);
            var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            var headerError = validateHeaders(sheet, formatter, evaluator);
            if (headerError != null) return failure(1, headerError);

            var questions = new ArrayList<CreateTrainingQuestionCommand>();
            var errors = new ArrayList<QuestionImportError>();
            int totalRows = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                if (isBlank(row, formatter, evaluator)) continue;
                totalRows++;
                if (totalRows > MAX_ROWS) {
                    errors.add(new QuestionImportError(rowIndex + 1, "单次最多导入 1000 道题目"));
                    break;
                }
                try {
                    questions.add(parseRow(row, formatter, evaluator));
                } catch (IllegalArgumentException exception) {
                    errors.add(new QuestionImportError(rowIndex + 1, exception.getMessage()));
                }
            }
            if (errors.isEmpty()) return new QuestionImportParseResult(totalRows, List.copyOf(questions), List.of());
            return new QuestionImportParseResult(totalRows, List.of(), List.copyOf(errors));
        }
    }

    private CreateTrainingQuestionCommand parseRow(
            Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        var type = parseType(value(row, 0, formatter, evaluator));
        var title = required(value(row, 1, formatter, evaluator), "题干不能为空");
        var options = type == QuestionType.TRUE_FALSE
                ? trueFalseOptions()
                : parseOptions(row, formatter, evaluator);
        var answers = parseAnswers(type, value(row, 6, formatter, evaluator));
        int score = parseScore(value(row, 7, formatter, evaluator));
        var category = required(value(row, 8, formatter, evaluator), "分类不能为空");
        var explanation = blankToNull(value(row, 9, formatter, evaluator));
        validateAnswers(type, options, answers);
        return new CreateTrainingQuestionCommand(type, title, options, answers, score, category, explanation);
    }

    private QuestionType parseType(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "单选", "单选题", "SINGLE_CHOICE" -> QuestionType.SINGLE_CHOICE;
            case "多选", "多选题", "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE;
            case "判断", "判断题", "TRUE_FALSE" -> QuestionType.TRUE_FALSE;
            default -> throw new IllegalArgumentException("题型仅支持单选、多选或判断");
        };
    }

    private Map<String, String> parseOptions(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        var options = new LinkedHashMap<String, String>();
        for (int index = 0; index < 4; index++) {
            var option = value(row, index + 2, formatter, evaluator);
            if (!option.isBlank()) options.put(String.valueOf((char) ('A' + index)), option.trim());
        }
        if (options.size() < 2) throw new IllegalArgumentException("单选和多选题至少需要两个选项");
        return options;
    }

    private Set<String> parseAnswers(QuestionType type, String value) {
        var raw = required(value, "正确答案不能为空").trim();
        if (type == QuestionType.TRUE_FALSE) {
            return switch (raw.toUpperCase(Locale.ROOT)) {
                case "正确", "对", "TRUE", "A", "1" -> Set.of("TRUE");
                case "错误", "错", "FALSE", "B", "0" -> Set.of("FALSE");
                default -> throw new IllegalArgumentException("判断题正确答案应为正确或错误");
            };
        }
        var answers = new LinkedHashSet<String>();
        for (String part : raw.split("[,，;；、\\s]+")) {
            if (!part.isBlank()) answers.add(part.trim().toUpperCase(Locale.ROOT));
        }
        return Set.copyOf(answers);
    }

    private int parseScore(String value) {
        try {
            var normalized = required(value, "分值不能为空").trim();
            var score = Integer.parseInt(normalized.endsWith(".0")
                    ? normalized.substring(0, normalized.length() - 2) : normalized);
            if (score < 1 || score > 1000) throw new NumberFormatException();
            return score;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("分值必须是 1 到 1000 的整数");
        }
    }

    private void validateAnswers(QuestionType type, Map<String, String> options, Set<String> answers) {
        if (answers.isEmpty() || !options.keySet().containsAll(answers)) {
            throw new IllegalArgumentException("正确答案必须对应已填写的选项");
        }
        if (type == QuestionType.SINGLE_CHOICE && answers.size() != 1) {
            throw new IllegalArgumentException("单选题只能有一个正确答案");
        }
        if (type == QuestionType.MULTIPLE_CHOICE && answers.size() < 2) {
            throw new IllegalArgumentException("多选题至少需要两个正确答案");
        }
    }

    private String validateHeaders(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        var row = sheet.getRow(0);
        if (row == null) return "缺少表头";
        for (int index = 0; index < HEADERS.size(); index++) {
            if (!HEADERS.get(index).equals(value(row, index, formatter, evaluator))) {
                return "第 %d 列表头应为“%s”".formatted(index + 1, HEADERS.get(index));
            }
        }
        return null;
    }

    private boolean isBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) return true;
        for (int index = 0; index < HEADERS.size(); index++) {
            if (!value(row, index, formatter, evaluator).isBlank()) return false;
        }
        return true;
    }

    private String value(Row row, int column, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null || row.getCell(column) == null) return "";
        return formatter.formatCellValue(row.getCell(column), evaluator).trim();
    }

    private Map<String, String> trueFalseOptions() {
        var options = new LinkedHashMap<String, String>();
        options.put("TRUE", "正确");
        options.put("FALSE", "错误");
        return options;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private QuestionImportParseResult failure(int row, String message) {
        return new QuestionImportParseResult(0, List.of(), List.of(new QuestionImportError(row, message)));
    }
}
