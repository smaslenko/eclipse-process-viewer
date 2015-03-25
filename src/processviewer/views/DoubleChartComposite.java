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
		// for (int i = 0; i < HISTORY_SIZE; i++) {
		// processHistory[i] = -1;
		// }
	}

	public static double getProcessCpuLoad() {
		AttributeList list = null;
		Double value = 0.0;

		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = ObjectName
					.getInstance("java.lang:type=OperatingSystem");
			list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

			if (list.isEmpty()) {
				return 0;
			}

			Attribute att = (Attribute) list.get(0);
			value = (Double) att.getValue();
			if (value == -1.0) {
				return 0;
			} // usually takes a couple of seconds before we get real values

		} catch (MalformedObjectNameException | InstanceNotFoundException
				| ReflectionException e) {
			e.printStackTrace();
		}

		return ((int) (value * 1000) / 10.0); // returns a percentage value with
												// 1 decimal point precision
	}

	// RESTRICTED method. Follow this link to solve trouble
	// http://stackoverflow.com/questions/860187/access-restriction-on-class-due-to-restriction-on-required-library-rt-jar
	public long getMaxRAMAmount() {
		com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		long maxRam =  bean.getTotalPhysicalMemorySize();
		return maxRam;
	}
	
	public long getFreeRAMAmount() {
		com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		long freeRam =  bean.getFreePhysicalMemorySize();
		return freeRam;
	}
	
	public double getUsagesMemoryInPercent() {
		double mb = 1024*1024; 
		
		double memoryUsed = (getMaxRAMAmount() - getFreeRAMAmount()) / mb;
		double maxRam = getMaxRAMAmount() / mb;
		double result =  ( ( 100 * memoryUsed / maxRam  ) / 100 );
		result = round(result, 2);
		return result;
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
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
			axisY.setRange(new Range(0, (getMaxRAMAmount() / mb) ));
		}
		
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

	public void updateProcessList(ArrayList<ProcessInfo> processList) {
		this.processList = processList;

		
		double memoryLoad = 0;

		for (ProcessInfo processInfo : processList) {
			memoryLoad += processInfo.memory;
		}
		
		System.out.println("Memory in percent: "+ getUsagesMemoryInPercent() +" %");
		
		if (index < HISTORY_SIZE) {
			cpuHistory[index] = getProcessCpuLoad();
			memoryHistory[index] = getUsagesMemoryInPercent();
			index++;

		} else {
			for (int i = 0; i < HISTORY_SIZE - 1; i++) {
				cpuHistory[i] = cpuHistory[i + 1];
				memoryHistory[i] = memoryHistory[i + 1];
			}
			cpuHistory[HISTORY_SIZE - 1] = getProcessCpuLoad();
			memoryHistory[HISTORY_SIZE - 1] = getUsagesMemoryInPercent();
		}

		memoryLoad = 0;

		String cpuSeriesId = "cpu load";
		String memorySeriesId = "memory load";

		if (index == 1) {
			setupLines(cpuSeriesId, memorySeriesId);
		}
		resetSeries(cpuSeriesId, memorySeriesId);
		setupLines(cpuSeriesId, memorySeriesId);

		System.out.println();

//		for (int i = 0; i < HISTORY_SIZE; i++) {
//			System.out.println("cpu load = " + cpuHistory[i]);
//		}
	
		for (int i = 0; i < HISTORY_SIZE; i++) {
			System.out.println("memory load = " + memoryHistory[i]);
		}
		
		double mb = 1024 * 1024; 
		double gb = 1024 * mb;
		System.out.println("Total memory      : " + getMaxRAMAmount() / mb +" Mb");
		System.out.println("Total memory Free : " + ( getFreeRAMAmount() / mb ) + " Mb");
		System.out.println("Total memory Usage: " + ( (getMaxRAMAmount() - getFreeRAMAmount()) / mb ) + " Mb");
		switch (type) {
		case ALL:

			break;
		case SINGLE:

			break;
		default:
			break;
		}
	}
	
	public void updateProcessList(ProcessInfo process) {
		this.currentProcess = process;
	
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
	
	public void resetProcess(){
		indexSingle = 0;
		for (int i = 0; i < HISTORY_SIZE ; i++) {
			cpuHistorySingle[i] = 0;
			memoryHistorySingle[i] = 0;
		}
	}

	private void setupLines(String cpuId, String memoryId) {

		ILineSeries lineSeries1 = (ILineSeries) cpuChart.getSeriesSet()
				.createSeries(SeriesType.LINE, cpuId);
		if(type == ChartType.SINGLE){
			lineSeries1.setYSeries(cpuHistorySingle);
		}else{
			lineSeries1.setYSeries(cpuHistory);	
		}
		
		lineSeries1.setLineColor(Display.getDefault().getSystemColor(
				SWT.COLOR_RED));
		lineSeries1.enableArea(false);
		lineSeries1.getLabel().setVisible(true);
		lineSeries1.setSymbolType(PlotSymbolType.NONE);
		lineSeries1.setLineWidth(2);
		
		ILineSeries lineSeries2 = (ILineSeries) memoryChart.getSeriesSet()
				.createSeries(SeriesType.LINE, memoryId);
		if(type == ChartType.SINGLE){
			lineSeries2.setYSeries(memoryHistorySingle);
		}else{
			lineSeries2.setYSeries(memoryHistory);
		}
		
		lineSeries2.setLineColor(Display.getDefault().getSystemColor(
				SWT.COLOR_RED));
		lineSeries2.enableArea(false);
		lineSeries2.getLabel().setVisible(true);
		lineSeries2.setSymbolType(PlotSymbolType.NONE);
		lineSeries2.setLineWidth(2);
		
		cpuChart.getAxisSet().adjustRange();
		memoryChart.getAxisSet().adjustRange();
	}

	private void resetSeries(String cpuId, String memoryId) {
		cpuChart.getSeriesSet().deleteSeries(cpuId);
		memoryChart.getSeriesSet().deleteSeries(memoryId);
	}

}
