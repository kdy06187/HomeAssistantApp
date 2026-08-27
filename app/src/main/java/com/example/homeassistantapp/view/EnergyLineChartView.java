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
import java.util.ArrayList;
import java.util.List;

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
    public void setEnergyData(List<EnergyLog> logs) {
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < logs.size(); i++) {
            // Y축 값: 전력량, X축 값: 인덱스 순서
            float energy = (float) logs.get(i).getTotalEnergy_mWh();
            entries.add(new Entry(i, energy));
        }

        LineDataSet dataSet = new LineDataSet(entries, "전력 사용량");
        dataSet.setColor(Color.parseColor("#4CAF50")); // 메인 그린 색상
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);                 // 부드러운 선을 위해 점 숨김
        dataSet.setMode(LineDataSet.Mode.LINEAR); // 직각 대신 곡선 처리
        dataSet.setDrawValues(false);                  // 차트 위 숫자 텍스트 숨김

        LineData lineData = new LineData(dataSet);
        this.setData(lineData);
        this.invalidate(); // 화면에 렌더링(새로고침)
    }
}
