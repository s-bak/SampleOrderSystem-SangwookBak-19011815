package org.example.ui;

import org.example.domain.Sample;
import org.example.service.SampleService;

import java.util.List;

public class SampleMenuHandler {

    private final ConsoleIO io;
    private final SampleService sampleService;

    public SampleMenuHandler(ConsoleIO io, SampleService sampleService) {
        this.io = io;
        this.sampleService = sampleService;
    }

    public void handle() {
        while (true) {
            io.println("\n--- 시료 관리 ---");
            io.println("1. 시료 등록");
            io.println("2. 시료 목록 조회");
            io.println("3. 시료 검색");
            io.println("0. 뒤로");
            io.print("선택> ");
            String input = io.readLine();

            switch (input) {
                case "1" -> register();
                case "2" -> listAll();
                case "3" -> search();
                case "0" -> { return; }
                default -> io.println("[오류] 올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void register() {
        String id;
        while (true) {
            io.print("시료 ID (예: S-001): ");
            id = io.readLine();
            if (sampleService.existsById(id)) {
                io.println("[오류] 이미 존재하는 시료 ID입니다: " + id);
            } else {
                break;
            }
        }
        io.print("시료명: ");
        String name = io.readLine();
        double avgTime = io.readDouble("평균 생산시간 (min, 0 초과): ");
        if (Double.isNaN(avgTime) || avgTime <= 0) {
            io.println("[오류] 평균 생산시간은 0 초과의 숫자여야 합니다.");
            return;
        }
        io.print("수율 (0 초과 1 이하): ");
        double yield;
        try {
            yield = Double.parseDouble(io.readLine());
        } catch (NumberFormatException e) {
            io.println("[오류] 올바른 수율을 입력해주세요.");
            return;
        }
        try {
            Sample s = sampleService.register(id, name, avgTime, yield, 0);
            io.println("등록 완료: [" + s.getId() + "] " + s.getName());
        } catch (Exception e) {
            io.println("[오류] " + e.getMessage());
        }
    }

    private void listAll() {
        List<Sample> list = sampleService.findAll();
        if (list.isEmpty()) {
            io.println("등록된 시료가 없습니다.");
            return;
        }
        printHeader();
        list.forEach(this::printRow);
        printSeparator();
    }

    private void search() {
        io.println("검색 항목:");
        io.println("1. 시료 ID");
        io.println("2. 시료명");
        io.println("3. 평균 생산시간");
        io.println("4. 수율");
        io.print("선택> ");
        String field = io.readLine();

        List<Sample> list;
        switch (field) {
            case "1" -> {
                io.print("시료 ID 검색어: ");
                list = sampleService.searchById(io.readLine());
            }
            case "2" -> {
                io.print("시료명 검색어: ");
                list = sampleService.search(io.readLine());
            }
            case "3" -> {
                double time = io.readDouble("평균 생산시간: ");
                if (Double.isNaN(time)) {
                    io.println("[오류] 올바른 숫자를 입력해주세요.");
                    return;
                }
                list = sampleService.searchByAvgProductionTime(time);
            }
            case "4" -> {
                double yield = io.readDouble("수율: ");
                if (Double.isNaN(yield)) {
                    io.println("[오류] 올바른 숫자를 입력해주세요.");
                    return;
                }
                list = sampleService.searchByYield(yield);
            }
            default -> {
                io.println("[오류] 올바른 항목 번호를 입력해주세요.");
                return;
            }
        }

        if (list.isEmpty()) {
            io.println("검색 결과가 없습니다.");
            return;
        }
        printHeader();
        list.forEach(this::printRow);
        printSeparator();
    }

    private void printSeparator() {
        io.println("+" + "-".repeat(8) + "+" + "-".repeat(17) + "+" + "-".repeat(15) + "+" + "-".repeat(8) + "+" + "-".repeat(8) + "+");
    }

    private void printHeader() {
        printSeparator();
        io.println("| " + padRight("시료ID", 6) +
                   " | " + padRight("시료명", 15) +
                   " | " + padLeft("생산시간(min)", 13) +
                   " | " + padLeft("수율", 6) +
                   " | " + padLeft("재고", 6) + " |");
        printSeparator();
    }

    private void printRow(Sample s) {
        io.println(String.format("| %-6s | %-15s | %13.1f | %6.2f | %6d |",
                s.getId(), s.getName(), s.getAvgProductionTime(), s.getYield(), s.getStock()));
    }

    // 한글은 시각적 폭 2, ASCII는 1로 계산
    private static int visualWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF) ? 2 : 1;
        }
        return w;
    }

    private static String padRight(String s, int targetVisual) {
        int pad = targetVisual - visualWidth(s);
        return pad > 0 ? s + " ".repeat(pad) : s;
    }

    private static String padLeft(String s, int targetVisual) {
        int pad = targetVisual - visualWidth(s);
        return pad > 0 ? " ".repeat(pad) + s : s;
    }
}
