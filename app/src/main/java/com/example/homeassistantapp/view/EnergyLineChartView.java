package com.example.homeassistantapp.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import com.example.homeassistantapp.dto.EnergyLog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class EnergyLineChartView extends LineChart {
    public EnergyLineChartView(Context context){
        super(context);
        initChart();
    }
    public EnergyLineChartView(Context context, AttributeSet attrs){
        super(context, attrs);
        initChart();
    }
    public EnergyLineChartView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initChart();
    }

    private void initChart(){
        getDescription().setEnabled(false);
        getLegend().setEnabled(false);
        getAxisRight().setEnabled(false);
        getXAxis().setDrawGridLines(false);
        getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        setTouchEnabled(true);
    }
    public void setEnergyData(List<EnergyLog> rawLogs) {
        List<Entry> entries = new ArrayList<>();

        // 서버는 최신순으로 준다.
        List<EnergyLog> logs = EnergyLogTimeline.oldestFirst(rawLogs);
        TimeZone localZone = TimeZone.getDefault();

        // 누적값 대신 이웃 기록의 차이를 그린다. usage[i]는 logs[i]~logs[i+1] 구간.
        List<Long> usage = EnergyLogTimeline.intervalUsage(logs);
        if (usage.isEmpty()) {
            setNoDataText("구간 사용량을 그리려면 기록이 2개 이상 필요합니다.");
            clear();
            return;
        }

        final String[] timeLabels = new String[usage.size()];
        for (int i = 0; i < usage.size(); i++) {
            entries.add(new Entry(i, (float) usage.get(i)));
            // 라벨은 구간이 끝난 시각, UTC를 현지 시간으로
            timeLabels[i] = EnergyLogTimeline.toLocalTimeLabel(logs.get(i + 1).getTimestamp(), localZone);
        }

        LineDataSet dataSet = new LineDataSet(entries, "구간 사용량 (mWh)");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        this.setData(lineData);
        XAxis xAxis = this.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timeLabels.length) {
                    return timeLabels[index];
                }
                return "";
            }
        });

        xAxis.setGranularity(1f);
        this.invalidate();
    }
}
