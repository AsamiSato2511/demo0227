package com.example.demo.service;

import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.ExamResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdviceService {

    public List<String> buildAdvice(List<ExamResult> recentResults,
                                    List<FieldImpact> impacts,
                                    List<CorrectRateSummary> bottleneckTop3,
                                    int remainToPass,
                                    int previousDiff) {
        List<String> lines = new ArrayList<>();
        if (remainToPass > 0) {
            lines.add("合格まであと" + remainToPass + "点。得点効率の高い分野から対策してください。");
        } else {
            lines.add("合格ライン到達中です。得点維持のため弱点分野を継続しましょう。");
        }

        if (!impacts.isEmpty()) {
            FieldImpact top = impacts.get(0);
            lines.add(top.getFieldName() + "を+10%改善すると約+" + top.getGainPointsForPlus10Rate() + "点見込みです。");
        }

        if (!bottleneckTop3.isEmpty()) {
            String name = bottleneckTop3.get(0).getName();
            Double rate = bottleneckTop3.get(0).getCorrectRate();
            lines.add("最優先ボトルネックは" + name + "（正答率 " + (rate != null ? rate : 0.0) + "%）です。");
        }

        if (recentResults.size() >= 2) {
            lines.add(previousDiff >= 0
                    ? "前回比で+" + previousDiff + "点。現ペースを維持して演習量を増やしましょう。"
                    : "前回比で" + previousDiff + "点。直近の誤答分野に絞って復習してください。");
        }
        return lines;
    }
}
