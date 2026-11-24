package com.example.homeassistantapp.view.detail;

import android.view.View;
import android.widget.TextView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.view.EnergyBarChartView;

/** 사용량 막대 차트 카드(제목·선택한 막대 값·빈 상태·안내). */
public final class ChartCard {
    private static final String SELECT_HINT = "막대를 누르면 값이 보여요";

    private final View root, empty, retry;
    private final TextView title, selected, emptyText, note;
    private final EnergyBarChartView chart;
    private ChartBars current;

    public ChartCard(View root, Runnable onRetry) {
        this.root = root;
        title = root.findViewById(R.id.tvChartTitle);
        selected = root.findViewById(R.id.tvChartSelected);
        chart = root.findViewById(R.id.detailBarChart);
        empty = root.findViewById(R.id.chartEmpty);
        emptyText = root.findViewById(R.id.tvChartEmpty);
        retry = root.findViewById(R.id.btnChartRetry);
        note = root.findViewById(R.id.tvChartNote);
        retry.setOnClickListener(v -> onRetry.run());
        chart.setOnBarSelected(index -> {
            if (current == null) return;
            selected.setText(index < 0 ? SELECT_HINT : current.describe(index));
        });
    }

    public void setVisible(boolean visible) {
        root.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** 불러오는 중. 이전 막대는 지우고 제목만 남긴다. */
    public void showLoading() {
        current = null;
        chart.setBars(ChartBars.failed(""));
        chart.setVisibility(View.INVISIBLE);
        empty.setVisibility(View.VISIBLE);
        emptyText.setText("불러오는 중…");
        retry.setVisibility(View.GONE);
        selected.setVisibility(View.GONE);
        note.setVisibility(View.GONE);
    }

    public void bind(ChartBars bars, boolean failed) {
        current = bars;
        title.setText(bars.title);
        boolean hasBars = bars.emptyText == null;
        chart.setVisibility(hasBars ? View.VISIBLE : View.INVISIBLE);
        chart.setBars(bars);
        empty.setVisibility(hasBars ? View.GONE : View.VISIBLE);
        emptyText.setText(bars.emptyText);
        retry.setVisibility(failed ? View.VISIBLE : View.GONE);
        selected.setVisibility(hasBars ? View.VISIBLE : View.GONE);
        selected.setText(SELECT_HINT);
        note.setVisibility(bars.note == null ? View.GONE : View.VISIBLE);
        note.setText(bars.note);
    }
}
