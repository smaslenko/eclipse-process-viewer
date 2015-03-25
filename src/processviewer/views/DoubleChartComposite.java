package processviewer.views;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxisTick;
import org.swtchart.IGrid;
import org.swtchart.ILegend;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;
import org.swtchart.internal.axis.AxisSet;

import processviewer.views.ProcessView.ProcessInfo;
import util.SystemUtils;

public class DoubleChartComposite extends Composite {
	public enum ChartType {
		ALL, SINGLE
	}

	final static Color COLOR_BLACK = new Color(Display.getDefault(), 0, 0, 0);
	final static Color COLOR_GREEN = new Color(Display.getDefault(), 0, 200, 0);
	final static int HISTORY_SIZE = 20;
	private Chart memoryChart;
	private Chart cpuChart;
	private int index;
	private int indexSingle;
	private ArrayList<ProcessInfo> processList;
	private ProcessInfo currentProcess;
	private double[] cpuHistory;
	private double[] memoryHistory;
	private double[] cpuHistorySingle;
	private double[] memoryHistorySingle;

	private ChartType type;

	public DoubleChartComposite(Composite parent, int style, ChartType type) {
		super(parent, style);
		setLayout(new RowLayout(SWT.NONE));

		this.type = type;

		cpuChart = new Chart(this, SWT.NONE);
		cpuChart.setLayoutData(new RowData(450, 150));
		cpuChart.getTitle().setText("Cpu Usage History");
		cpuChart.getTitle().setForeground(COLOR_GREEN);

		memoryChart = new Chart(this, SWT.NONE);
		memoryChart.setLayoutData(new RowData(450, 150));
		memoryChart.getTitle().setText("Memory Usage History");
		memoryChart.getTitle().setForeground(COLOR_GREEN);

		adjustColors(cpuChart);
		adjustColors(memoryChart);
		setupAxises(cpuChart);
		setupAxises(memoryChart);

		cpuHistory = new double[HISTORY_SIZE];
		memoryHistory = new double[HISTORY_SIZE];
		cpuHistorySingle = new double[HISTORY_SIZE];
		memoryHistorySingle = new double[HISTORY_SIZE];
	}
	
	
	public Chart getMemoryChart() {
		return memoryChart;
	}

	public Chart getCpuChart() {
		return cpuChart;
	}

	private void adjustColors(Chart chart) {
		chart.setBackground(COLOR_BLACK);
		chart.setBackgroundInPlotArea(COLOR_BLACK);
	}

	private void setupAxises(Chart chart) {

		AxisSet set = (AxisSet) chart.getAxisSet();
		
		IAxis axisY = set.getYAxis(0);
		if(chart.getTitle().getText().equals("Cpu Usage History")){
			axisY.setRange(new Range(0, 100));
		}else{
			double mb = 1024 * 1024;
			axisY.setRange(new Range(0, (SystemUtils.getMaxRAMAmount() / mb) ));
		}
		axisY.zoomOut();
		
		IAxisTick xTick = set.getXAxis(0).getTick();
		xTick.setVisible(false);
		xTick.setTickMarkStepHint(25);
		
		IAxisTick yTick = set.getYAxis(0).getTick();
		yTick.setVisible(true);
		yTick.setTickMarkStepHint(25);
		yTick.setForeground(COLOR_GREEN);

		IGrid xGrid = set.getXAxis(0).getGrid();
		IGrid yGrid = set.getYAxis(0).getGrid();

		xGrid.setForeground(COLOR_GREEN);
		yGrid.setForeground(COLOR_GREEN);

		set.getXAxis(0).getTitle().setVisible(false);
		set.getYAxis(0).getTitle().setVisible(false);
		
//		chart.getLegend().setPosition(SWT.TOP);
//		chart.getAxisSet().getXAxis(0).getTitle().setText("Time (10 seconds)");
//		chart.getAxisSet().getYAxis(0).getTitle().setText("Percent");
	}

	public void updateProcessList() {
		
		System.out.println("CPU in percent    : "+ SystemUtils.getProcessCpuLoad2() +" %");
		
		if (index < HISTORY_SIZE) {
			cpuHistory[index] = SystemUtils.getProcessCpuLoad2();
			memoryHistory[index] = SystemUtils.getUsagesMemoryInPercent();
			index++;

		} else {
			for (int i = 0; i < HISTORY_SIZE - 1; i++) {
				cpuHistory[i] = cpuHistory[i + 1];
				memoryHistory[i] = memoryHistory[i + 1];
			}
			cpuHistory[HISTORY_SIZE - 1] = SystemUtils.getProcessCpuLoad2();
			memoryHistory[HISTORY_SIZE - 1] = SystemUtils.getUsagesMemoryInPercent();
		}

		String cpuSeriesId = "cpu load";
		String memorySeriesId = "memory load";

		if (index == 1) {
			setupLines(cpuSeriesId, memorySeriesId);
		}
		resetSeries(cpuSeriesId, memorySeriesId);
		setupLines(cpuSeriesId, memorySeriesId);
	
		for (int i = 0; i < HISTORY_SIZE; i++) {
			System.out.println("memory load = " + memoryHistory[i]);
		}

		
		double mb = 1024 * 1024; 
		double gb = 1024 * mb;
		System.out.println("Total memory      : " + SystemUtils.getMaxRAMAmount() / mb +" Mb");
		System.out.println("Total memory Free : " + ( SystemUtils.getFreeRAMAmount() / mb ) + " Mb");
		System.out.println("Total memory Usage: " + ( (SystemUtils.getMaxRAMAmount() - SystemUtils.getFreeRAMAmount()) / mb ) + " Mb");

	}
	
	public void updateProcessList(ProcessInfo process) {
		this.currentProcess = process;
		
		System.out.println();
		if (indexSingle < HISTORY_SIZE) {
			cpuHistorySingle[indexSingle] = 1.0 * process.cpu* 100 /ProcessView.totalCpu;
			memoryHistorySingle[indexSingle] = currentProcess.memory;
			indexSingle++;

		} else {
			for (int i = 0; i < HISTORY_SIZE - 1; i++) {
				cpuHistorySingle[i] = cpuHistorySingle[i + 1];
				memoryHistorySingle[i] = memoryHistorySingle[i + 1];
			}
			cpuHistorySingle[HISTORY_SIZE - 1] = 1.0 * process.cpu* 100 /ProcessView.totalCpu;
			memoryHistorySingle[HISTORY_SIZE - 1] = process.memory;
		}

		String cpuSeriesId = "cpu load";
		String memorySeriesId = "memory load";

		if (indexSingle == 1) {
			setupLines(cpuSeriesId, memorySeriesId);
		}
		resetSeries(cpuSeriesId, memorySeriesId);
		setupLines(cpuSeriesId, memorySeriesId);

		
//		System.out.println("CPU Single process : "  + this.currentProcess.name);
//		System.out.println("-------------------------------------");
//		for (int i = 0; i < HISTORY_SIZE; i++) {
//			System.out.println("cpu load = " + cpuHistorySingle[i]);
//		}
//		
//		System.out.println("MEMORY Single process : "  + this.currentProcess.name);
//		
//		for (int i = 0; i < HISTORY_SIZE; i++) {
//			System.out.println("memory load = " + memoryHistorySingle[i]);
//		}
	}
	
	public void setProcessName(String name){
		cpuChart.getTitle().setText(name + " Cpu Usage History ");
		cpuChart.redraw();
		memoryChart.getTitle().setText(name + " Memory Usage History");
		memoryChart.redraw();
	}
	
	public void resetProcess(){
		indexSingle = 0;
		cpuHistorySingle = new double[HISTORY_SIZE];
		memoryHistorySingle = new double[HISTORY_SIZE];
	}

	private void setupLines(String cpuId, String memoryId) {

		ILineSeries lineSeries1 = (ILineSeries) cpuChart.getSeriesSet().createSeries(SeriesType.LINE, cpuId);
		if(type == ChartType.SINGLE){
			lineSeries1.setYSeries(cpuHistorySingle);
		}else{
			lineSeries1.setYSeries(cpuHistory);	
		}
		
		lineSeries1.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		lineSeries1.enableArea(false);
		lineSeries1.getLabel().setVisible(true);
		lineSeries1.setSymbolType(PlotSymbolType.NONE);
		lineSeries1.setLineWidth(2);
		
		ILineSeries lineSeries2 = (ILineSeries) memoryChart.getSeriesSet().createSeries(SeriesType.LINE, memoryId);
		if(type == ChartType.SINGLE){
			lineSeries2.setYSeries(memoryHistorySingle);
		}else{
			lineSeries2.setYSeries(memoryHistory);
		}
		
		lineSeries2.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		lineSeries2.enableArea(false);
		lineSeries2.getLabel().setVisible(true);
		lineSeries2.setSymbolType(PlotSymbolType.NONE);
		lineSeries2.setLineWidth(2);
		
		cpuChart.getAxisSet().adjustRange();
		memoryChart.getAxisSet().adjustRange();
		
		cpuChart.redraw();
		memoryChart.redraw();
//		cpuChart.getAxisSet().zoomOut();
//		memoryChart.getAxisSet().zoomOut();
	
	}

	private void resetSeries(String cpuId, String memoryId) {
		cpuChart.getSeriesSet().deleteSeries(cpuId);
		memoryChart.getSeriesSet().deleteSeries(memoryId);
	}

}
