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
        int avgTime = io.readInt("평균 생산시간 (min): ");
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
    }

    private void search() {
        io.print("검색 키워드: ");
        String keyword = io.readLine();
        List<Sample> list = sampleService.search(keyword);
        if (list.isEmpty()) {
            io.println("검색 결과가 없습니다.");
            return;
        }
        printHeader();
        list.forEach(this::printRow);
    }

    private void printHeader() {
        io.println(String.format("%-8s %-15s %10s %6s %6s",
                "시료ID", "시료명", "생산시간(min)", "수율", "재고"));
        io.println("-".repeat(52));
    }

    private void printRow(Sample s) {
        io.println(String.format("%-8s %-15s %12d %6.2f %6d",
                s.getId(), s.getName(), s.getAvgProductionTime(), s.getYield(), s.getStock()));
    }
}
