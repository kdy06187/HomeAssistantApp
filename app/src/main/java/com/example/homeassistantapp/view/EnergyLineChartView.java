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
        getLegend().setEnabled(false);         // 범례 숨김
        getAxisRight().setEnabled(false);      // 우측 Y축 숨김
        getXAxis().setDrawGridLines(false);    // 세로 눈금선 숨김
        getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // X축을 아래로
        setTouchEnabled(true);
    }
    public void setEnergyData(List<EnergyLog> rawLogs) {
        List<Entry> entries = new ArrayList<>();

        // 불일치 #10: 서버는 최신순으로 보내므로, 시간축이 왼쪽→오른쪽으로 흐르게 오래된 순으로 바꾼다.
        List<EnergyLog> logs = EnergyLogTimeline.oldestFirst(rawLogs);
        TimeZone localZone = TimeZone.getDefault();

        final String[] timeLabels = new String[logs.size()];
        for (int i = 0; i < logs.size(); i++) {
            // Y축 값: 전력량, X축 값: 인덱스 순서
            float energy = (float) logs.get(i).getTotalEnergy_mWh();
            entries.add(new Entry(i, energy));
            // 불일치 #9: 서버 시각은 UTC라서 폰의 현지 시간으로 바꿔 표시한다(읽기 실패 시 빈칸).
            timeLabels[i] = EnergyLogTimeline.toLocalTimeLabel(logs.get(i).getTimestamp(), localZone);
        }

        LineDataSet dataSet = new LineDataSet(entries, "전력 사용량");
        dataSet.setColor(Color.parseColor("#4CAF50")); // 메인 그린 색상
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);                 // 부드러운 선을 위해 점 숨김
        dataSet.setMode(LineDataSet.Mode.LINEAR); // 직각 대신 곡선 처리
        dataSet.setDrawValues(false);                  // 차트 위 숫자 텍스트 숨김

        LineData lineData = new LineData(dataSet);
        this.setData(lineData);
        XAxis xAxis = this.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                // value(X축 좌표)를 인덱스로 삼아, 저장해둔 시간 배열에서 꺼내옵니다.
                if (index >= 0 && index < timeLabels.length) {
                    return timeLabels[index];
                }
                return "";
            }
        });

        xAxis.setGranularity(1f);
        this.invalidate(); // 화면에 렌더링(새로고침)
    }
}
