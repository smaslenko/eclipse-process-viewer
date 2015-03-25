package processviewer.views;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import processviewer.views.DoubleChartComposite.ChartType;
import processviewer.views.ProcessView.ProcessInfo;

public class ChartsPanelComposite extends Composite {
	private DoubleChartComposite doubleGraphCompositeAll;
	private DoubleChartComposite doubleGraphCompositeSingle;

	public ChartsPanelComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new RowLayout(SWT.VERTICAL));

		doubleGraphCompositeAll = new DoubleChartComposite(this, SWT.NONE,
				ChartType.ALL);

		Label label = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new RowData(468, 9));

		doubleGraphCompositeSingle = new DoubleChartComposite(this, SWT.NONE,
				ChartType.SINGLE);

	}

	public DoubleChartComposite getDoubleGraphCompositeAll() {
		return doubleGraphCompositeAll;
	}

	public DoubleChartComposite getDoubleGraphCompositeSelected() {
		return doubleGraphCompositeSingle;
	}

	public void updateProcessList() {
		doubleGraphCompositeAll.updateProcessList();
	}
	
	public void updateCurrentProcess(ProcessInfo process){
		doubleGraphCompositeSingle.updateProcessList(process);
	}
	
	public void resetCurrentProcess(){
		doubleGraphCompositeSingle.resetProcess();
	}
}
