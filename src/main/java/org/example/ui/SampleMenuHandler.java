package org.example.ui;

import org.example.domain.Sample;
import org.example.repository.JsonDataStore;
import org.example.service.SampleService;

import java.util.List;

public class SampleMenuHandler {

    private final ConsoleIO io;
    private final SampleService sampleService;
    private final JsonDataStore dataStore;

    public SampleMenuHandler(ConsoleIO io, SampleService sampleService, JsonDataStore dataStore) {
        this.io = io;
        this.sampleService = sampleService;
        this.dataStore = dataStore;
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
            if (id.isEmpty()) {
                io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
                return;
            }
            if (sampleService.existsById(id)) {
                io.println("[오류] 이미 존재하는 시료 ID입니다: " + id);
            } else {
                break;
            }
        }
        String name;
        while (true) {
            io.print("시료명: ");
            name = io.readLine();
            if (name.isEmpty()) {
                io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
                return;
            }
            if (!name.isBlank()) break;
            io.println("[오류] 시료명은 공백일 수 없습니다.");
        }
        io.print("평균 생산시간 (min, 0 초과): ");
        String avgTimeStr = io.readLine();
        if (avgTimeStr.isEmpty()) {
            io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
            return;
        }
        double avgTime;
        try {
            avgTime = Double.parseDouble(avgTimeStr);
        } catch (NumberFormatException e) {
            io.println("[오류] 올바른 숫자를 입력해주세요.");
            return;
        }
        if (avgTime <= 0) {
            io.println("[오류] 평균 생산시간은 0 초과의 숫자여야 합니다.");
            return;
        }
        io.print("수율 (0 초과 1 이하): ");
        String yieldStr = io.readLine();
        if (yieldStr.isEmpty()) {
            io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
            return;
        }
        double yield;
        try {
            yield = Double.parseDouble(yieldStr);
        } catch (NumberFormatException e) {
            io.println("[오류] 올바른 수율을 입력해주세요.");
            return;
        }
        try {
            Sample s = sampleService.register(id, name, avgTime, yield, 0);
            dataStore.save();
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
        if (field.isEmpty()) {
            io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
            return;
        }

        List<Sample> list;
        switch (field) {
            case "1" -> {
                io.print("시료 ID 검색어: ");
                String keyword = io.readLine();
                if (keyword.isEmpty()) {
                    io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
                    return;
                }
                list = sampleService.searchById(keyword);
            }
            case "2" -> {
                io.print("시료명 검색어: ");
                String keyword = io.readLine();
                if (keyword.isEmpty()) {
                    io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
                    return;
                }
                list = sampleService.search(keyword);
            }
            case "3" -> {
                io.print("평균 생산시간: ");
                String timeStr = io.readLine();
                if (timeStr.isEmpty()) {
                    io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
                    return;
                }
                double time;
                try {
                    time = Double.parseDouble(timeStr);
                } catch (NumberFormatException e) {
                    io.println("[오류] 올바른 숫자를 입력해주세요.");
                    return;
                }
                list = sampleService.searchByAvgProductionTime(time);
            }
            case "4" -> {
                io.print("수율: ");
                String yieldStr = io.readLine();
                if (yieldStr.isEmpty()) {
                    io.println("[안내] 입력이 없어 이전 메뉴로 돌아갑니다.");
                    return;
                }
                double yield;
                try {
                    yield = Double.parseDouble(yieldStr);
                } catch (NumberFormatException e) {
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
        io.println(String.format("| %-6s | %-15s | %13s | %6s | %6s |",
                "시료ID", "시료명", "생산시간(min)", "수율", "재고"));
        printSeparator();
    }

    private void printRow(Sample s) {
        io.println(String.format("| %-6s | %-15s | %13.1f | %6.2f | %6d |",
                s.getId(), s.getName(), s.getAvgProductionTime(), s.getYield(), s.getStock()));
    }
}
