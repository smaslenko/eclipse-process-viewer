package processviewer.views;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.plaf.TableHeaderUI;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;



public class ProcessView extends ViewPart {

	public static final String ID = "processviewer.views.ProcessView";

	private TableViewer viewer;
	private Action killAction;
	private MouseEvent lastEvent;
	
	private ArrayList<ProcessInfo> processList;
	private int totalCpu = 0;
	private Timer updateTimer;

	class ProcessInfo{
		public String name;
		public int cpu;
		public int pid;
		public ProcessInfo(String name, int cpu, int pid){
			this.name = name;
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
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
		public void dispose() {}
		public Object[] getElements(Object parent) {
			return processList.toArray();
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			ProcessInfo process = (ProcessInfo) obj;
			String result = "";
			
			if( 0 == index ){
				result = process.name;
			} else if ( 1 == index ) {
				if( 0 == totalCpu ){
					result = "0.00%";
				} else {
					result = new DecimalFormat("#.##").format(1.0 * process.cpu * 100 / totalCpu) + "%";
				}
			}
			
			return result;
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}
	
	private synchronized void updateProcessList(){
		try {
	        Process process = Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\tasklist.exe /NH /V /FO csv");
	        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        processList.removeAll(processList);

	        totalCpu = 0;
	        String line;
	        while ( null != (line = input.readLine()) ) {
	        	String temp = line.trim().substring(1);
	        	String[] task = temp.split("\",\"");
	        	
	        	// [0] process name
	        	// [7] cpu time in hh:mm:ss format
	        	if( 8 <= task.length ){
	        		int cpu = cpuTimeToInt(task[7]);
	        		totalCpu += cpu;
	        		processList.add( new ProcessInfo( task[0], cpu, Integer.parseInt(task[1]) ) );
	        	}
	        }
	        
	        Collections.sort(processList, new Comparator<ProcessInfo>() {

				@Override
				public int compare(ProcessInfo o1, ProcessInfo o2) {
					if( o1.cpu < o2.cpu )
						return 1;
					else if( o1.cpu > o2.cpu )
						return -1;
					else
						return 0;
				}
	        	
			});
	        input.close();
	    } catch (Exception err) {
	        err.printStackTrace();
	    }
		
		Display.getDefault().asyncExec(new Runnable() {
            public void run() {
            	viewer.refresh();
            }
         });
		
	}
	
	private int cpuTimeToInt(String time){
		int result = 0;
		String[] timeParts = time.split(":");
		
		if( 3 == timeParts.length ){
			result = Integer.parseInt(timeParts[0]) * 3600 + Integer.parseInt(timeParts[1]) * 60 + Integer.parseInt(timeParts[2]); 
		}
		
		return result;
	}

	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(getViewSite());
		
		viewer.getTable().setHeaderVisible(true);
		
		TableColumn columnName = new TableColumn(viewer.getTable(), SWT.LEFT);
		columnName.setText("Process name");
		columnName.setWidth(200);
		TableColumn columnCpu = new TableColumn(viewer.getTable(), SWT.LEFT);
		columnCpu.setText("CPU");
		columnCpu.setWidth(125);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "ProcessViewer.viewer");
		createActions();
		hookContextMenu();
	}

	private void hookContextMenu() {
		MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ViewerCell cell = viewer.getCell(new Point(ProcessView.this.lastEvent.x, ProcessView.this.lastEvent.y));
				if( null != cell ){
					manager.add(killAction);
				}
			}
		});
		
		Menu menu = menuManager.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		viewer.getControl().addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {}
			@Override
			public void mouseUp(MouseEvent e) {}
			@Override
			public void mouseDown(MouseEvent e) {
				if( 3 == e.button ){
					ProcessView.this.lastEvent = e;
				}
			}
		});
		getSite().registerContextMenu(menuManager, viewer);
	}

	private void createActions() {
		killAction = new Action() {
			public void run() {
				ViewerCell cell = viewer.getCell(new Point(ProcessView.this.lastEvent.x, ProcessView.this.lastEvent.y));
				if(null != cell){
					try {
						ProcessInfo process = (ProcessInfo)cell.getViewerRow().getElement();
						Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\taskkill.exe /F /PID " + process.pid);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		killAction.setText("Try ending process");
		killAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
}