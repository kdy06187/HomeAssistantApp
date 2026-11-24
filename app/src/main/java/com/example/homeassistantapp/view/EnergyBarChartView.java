package com.example.homeassistantapp.view;

import android.content.Context;
import android.util.AttributeSet;

import com.example.homeassistantapp.view.detail.ChartBars;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/** 사용량 막대 차트. 값 계산은 ChartBars가 하고 여기서는 그리기와 막대 선택만 한다. */
public class EnergyBarChartView extends BarChart {

    public interface OnBarSelected {
        /** index가 -1이면 선택이 풀림. */
        void onBarSelected(int index);
    }

    private OnBarSelected onBarSelected;

    public EnergyBarChartView(Context context) {
        super(context);
        initChart();
    }

    public EnergyBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChart();
    }

    public EnergyBarChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initChart();
    }

    private void initChart() {
        getDescription().setEnabled(false);
        getLegend().setEnabled(false);
        getAxisRight().setEnabled(false);
        setScaleEnabled(false);
        setDoubleTapToZoomEnabled(false);
        setNoDataText("");
        getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // 차트 라이브러리는 테마를 모르므로 축 글자·격자 색을 직접 넣는다.
        int labelColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant);
        float textSp = 12f;
        xAxis.setTextColor(labelColor);
        xAxis.setTextSize(textSp);
        xAxis.setAxisLineColor(gridColor);
        getAxisLeft().setTextColor(labelColor);
        getAxisLeft().setTextSize(textSp);
        getAxisLeft().setAxisLineColor(gridColor);
        getAxisLeft().setGridColor(gridColor);

        setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (onBarSelected != null) onBarSelected.onBarSelected((int) e.getX());
            }

            @Override
            public void onNothingSelected() {
                if (onBarSelected != null) onBarSelected.onBarSelected(-1);
            }
        });
    }

    public void setOnBarSelected(OnBarSelected listener) {
        onBarSelected = listener;
    }

    public void setBars(ChartBars chartBars) {
        highlightValues(null);
        if (chartBars.bars.isEmpty()) {
            clear();
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        final String[] labels = new String[chartBars.bars.size()];
        for (int i = 0; i < chartBars.bars.size(); i++) {
            ChartBars.Bar bar = chartBars.bars.get(i);
            entries.add(new BarEntry(i, bar.hasData ? chartBars.unit.scale(bar.energyMwh) : 0f));
            labels[i] = bar.label;
        }

        BarDataSet dataSet = new BarDataSet(entries, chartBars.title);
        dataSet.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        dataSet.setHighLightColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        setData(data);
        getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                return index >= 0 && index < labels.length ? labels[index] : "";
            }
        });
        setContentDescription(chartBars.summary());
        invalidate();
    }
}
