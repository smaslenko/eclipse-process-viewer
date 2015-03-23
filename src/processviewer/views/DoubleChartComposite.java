package processviewer.views;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxisTick;
import org.swtchart.IGrid;
import org.swtchart.internal.axis.AxisSet;

import processviewer.views.ProcessView.ProcessInfo;

public class DoubleChartComposite extends Composite {
	public enum ChartType {
		ALL, SINGLE
	}

	final static Color COLOR_BLACK = new Color(Display.getDefault(), 0, 0, 0);
	final static Color COLOR_GREEN = new Color(Display.getDefault(), 0, 200, 0);
	private Chart memoryChart;
	private Chart cpuChart;
	private ArrayList<ProcessInfo> processList;
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
		
		switch (type) {
		case ALL:
			
			break;
		case SINGLE:
			
			break;
		default:
			break;
		}

	}
}
