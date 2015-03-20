package processviewer.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ProcessView extends ViewPart {

	public static final String ID = "processviewer.views.ProcessView";

	private TableViewer tableViewer;
	private ChartsPanelComposite chartsPanel;
	private Action killAction;
	private MouseEvent lastEvent;
	private ProcessesTableComparator comparator;

	private ArrayList<ProcessInfo> processList;
	private int totalCpu = 0;
	private Timer updateTimer;

	class ProcessInfo {
		public String name;
		public double memory;
		public int cpu;
		public int pid;

		public ProcessInfo(String name, double memory, int cpu, int pid) {
			this.name = name;
			this.memory = memory;
			this.cpu = cpu;
			this.pid = pid;
		}
	}

	public ProcessView() {
		processList = new ArrayList<ProcessInfo>();
		updateTimer = new Timer();
		updateTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				updateProcessList();
			}
		}, 1000, 5000);
	}

	class ViewContentProvider implements IStructuredContentProvider {
		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object parent) {
			return processList.toArray();
		}
	}

	class ViewLabelProvider extends LabelProvider implements
	ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			ProcessInfo process = (ProcessInfo) obj;
			String result = "";

			switch (index) {
			case 0:
				result = process.name;
				break;
			case 1:
				result = process.memory + " K";
				break;
			case 2:
				if (0 == totalCpu) {
					result = "0.00%";
				} else {
					result = new DecimalFormat("#.##").format(1.0 * process.cpu
							* 100 / totalCpu)
							+ "%";
				}
				break;
			}
			return result;
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	private synchronized void updateProcessList() {
		try {
			Process process = Runtime.getRuntime().exec(
					System.getenv("windir")
					+ "\\system32\\tasklist.exe /NH /V /FO csv");
			BufferedReader input = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			processList.removeAll(processList);

			totalCpu = 0;
			String line;
			while (null != (line = input.readLine())) {
				String temp = line.trim().substring(1);
				String[] task = temp.split("\",\"");

				// [0] process name
				// [7] cpu time in hh:mm:ss format
				// [4] memory used
				if (8 <= task.length) {
					int cpu = cpuTimeToInt(task[7]);
					totalCpu += cpu;
					processList.add(new ProcessInfo(task[0],
							cpuMemoryToDouble(task[4]), cpu, Integer
							.parseInt(task[1])));
				}
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				tableViewer.refresh();
				chartsPanel.updateProcessList(processList);
			}
		});

	}

	private int cpuTimeToInt(String time) {
		int result = 0;
		String[] timeParts = time.split(":");

		if (3 == timeParts.length) {
			result = Integer.parseInt(timeParts[0]) * 3600
					+ Integer.parseInt(timeParts[1]) * 60
					+ Integer.parseInt(timeParts[2]);
		}

		return result;
	}

	private double cpuMemoryToDouble(String memoryOrig) {
		String memory = memoryOrig.substring(0, memoryOrig.indexOf(" "));
		NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
		Number number;
		try {
			number = format.parse(memory);
			return number.doubleValue();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public void createPartControl(Composite parent) {
		getViewSite().getShell().setMaximized(true);
		createTableView(parent);
		chartsPanel = new ChartsPanelComposite(parent, SWT.NONE);
	}

	private void createTableView(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setInput(getViewSite());
		tableViewer.getTable().setHeaderVisible(true);

		comparator = new ProcessesTableComparator();
		tableViewer.setComparator(comparator);

		TableColumn columnName = new TableColumn(tableViewer.getTable(),
				SWT.LEFT);
		columnName.setText("Process name");
		columnName.setWidth(200);
		columnName.addSelectionListener(getSelectionAdapter(columnName, 0));

		TableColumn columnMemory = new TableColumn(tableViewer.getTable(),
				SWT.LEFT);
		columnMemory.setText("Memory");
		columnMemory.setWidth(125);
		columnMemory.addSelectionListener(getSelectionAdapter(columnMemory, 1));

		TableColumn columnCpu = new TableColumn(tableViewer.getTable(),
				SWT.LEFT);
		columnCpu.setText("CPU");
		columnCpu.setWidth(125);
		columnCpu.addSelectionListener(getSelectionAdapter(columnCpu, 2));

		PlatformUI.getWorkbench().getHelpSystem()
		.setHelp(tableViewer.getControl(), "ProcessViewer.viewer");
		createActions();
		hookContextMenu();
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column,
			final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				tableViewer.getTable().setSortDirection(dir);
				tableViewer.getTable().setSortColumn(column);
				tableViewer.refresh();
			}
		};
		return selectionAdapter;
	}

	private void hookContextMenu() {
		MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				ViewerCell cell = tableViewer.getCell(new Point(
						ProcessView.this.lastEvent.x,
						ProcessView.this.lastEvent.y));
				if (null != cell) {
					manager.add(killAction);
				}
			}
		});

		Menu menu = menuManager.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		tableViewer.getControl().addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}

			@Override
			public void mouseUp(MouseEvent e) {
			}

			@Override
			public void mouseDown(MouseEvent e) {
				if (3 == e.button) {
					ProcessView.this.lastEvent = e;
				}
			}
		});
		getSite().registerContextMenu(menuManager, tableViewer);
	}

	private void createActions() {
		killAction = new Action() {
			@Override
			public void run() {
				ViewerCell cell = tableViewer.getCell(new Point(
						ProcessView.this.lastEvent.x,
						ProcessView.this.lastEvent.y));
				if (null != cell) {
					try {
						ProcessInfo process = (ProcessInfo) cell.getViewerRow()
								.getElement();
						Runtime.getRuntime().exec(
								System.getenv("windir")
								+ "\\system32\\taskkill.exe /F /PID "
								+ process.pid);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		killAction.setText("Try ending process");
		killAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}
}