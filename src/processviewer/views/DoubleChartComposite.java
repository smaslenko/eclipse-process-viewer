package processviewer.views;

import java.lang.management.ManagementFactory;
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
import org.swtchart.IAxisTick;
import org.swtchart.IGrid;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries.SeriesType;
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
	private ArrayList<ProcessInfo> processList;
	private double[] cpuHistory;
	private double[] memoryHistory;

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

		IAxisTick xTick = set.getXAxis(0).getTick();
		xTick.setVisible(false);
		IAxisTick yTick = set.getYAxis(0).getTick();
		yTick.setVisible(false);

		IGrid xGrid = set.getXAxis(0).getGrid();
		IGrid yGrid = set.getYAxis(0).getGrid();

		xGrid.setForeground(COLOR_GREEN);
		yGrid.setForeground(COLOR_GREEN);

		set.getXAxis(0).getTitle().setVisible(false);
		set.getYAxis(0).getTitle().setVisible(false);
	}

	public void updateProcessList(ArrayList<ProcessInfo> processList) {
		this.processList = processList;

		
		double memoryLoad = 0;

		for (ProcessInfo processInfo : processList) {
			memoryLoad += processInfo.memory;
		}
		
		if (index < HISTORY_SIZE) {
			cpuHistory[index] =getProcessCpuLoad();
			memoryHistory[index] = memoryLoad;
			index++;

		} else {
			for (int i = 0; i < HISTORY_SIZE - 1; i++) {
				cpuHistory[i] = cpuHistory[i + 1];
				memoryHistory[i] = memoryHistory[i + 1];
			}
			cpuHistory[HISTORY_SIZE - 1] = getProcessCpuLoad();
			memoryHistory[HISTORY_SIZE - 1] = memoryLoad;
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

		for (int i = 0; i < HISTORY_SIZE; i++) {
			System.out.println("cpu load = " + cpuHistory[i]);
		}

		System.out.println();

		switch (type) {
		case ALL:

			break;
		case SINGLE:

			break;
		default:
			break;
		}
	}

	private void setupLines(String cpuId, String memoryId) {

		ILineSeries lineSeries1 = (ILineSeries) cpuChart.getSeriesSet()
				.createSeries(SeriesType.LINE, cpuId);
		lineSeries1.setYSeries(cpuHistory);
		lineSeries1.setLineColor(Display.getDefault().getSystemColor(
				SWT.COLOR_RED));
		lineSeries1.enableArea(true);
		lineSeries1.getLabel().setVisible(true);

		ILineSeries lineSeries2 = (ILineSeries) memoryChart.getSeriesSet()
				.createSeries(SeriesType.LINE, memoryId);
		lineSeries2.setYSeries(memoryHistory);
		lineSeries2.setLineColor(Display.getDefault().getSystemColor(
				SWT.COLOR_RED));
		lineSeries2.enableArea(true);
		lineSeries2.getLabel().setVisible(true);

		cpuChart.getAxisSet().adjustRange();
		memoryChart.getAxisSet().adjustRange();
	}

	private void resetSeries(String cpuId, String memoryId) {
		cpuChart.getSeriesSet().deleteSeries(cpuId);
		memoryChart.getSeriesSet().deleteSeries(memoryId);
	}
}
