package com.example.demo.service;

import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.ExamResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdviceService {

    public List<String> buildAdvice(List<ExamResult> recentResults,
                                    List<CorrectRateSummary> fieldRates,
                                    List<CorrectRateSummary> bottleneckTop3,
                                    int remainToPass,
                                    int previousDiff) {
        List<String> lines = new ArrayList<>();

        if (remainToPass > 0) {
            lines.add("合格ラインまであと" + remainToPass + "点。弱点分野の取りこぼし回収が最短ルートです。");
        } else {
            lines.add("合格ラインを超えています。弱点分野を1つずつ潰して安定化しましょう。");
        }

        if (!bottleneckTop3.isEmpty()) {
            CorrectRateSummary weakest = bottleneckTop3.get(0);
            double rate = weakest.getCorrectRate() != null ? weakest.getCorrectRate() : 0.0;
            lines.add("最優先は「" + weakest.getName() + "」（正答率 " + rate + "%）。まずここを10問実施。");
        }

        if (recentResults.size() >= 2) {
            lines.add(previousDiff >= 0
                    ? "直近は +" + previousDiff + "点で改善傾向。現在の学習リズムを維持してください。"
                    : "直近は " + previousDiff + "点。今日中に間違い復習を1セット入れましょう。");
        }

        if (!fieldRates.isEmpty()) {
            CorrectRateSummary strongest = fieldRates.get(0);
            lines.add("今日やること: 弱点10問 -> 間違い復習 -> 「" + strongest.getName() + "」で成功体験を作る。");
        }
        return lines;
    }
}
