package processviewer.views;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

import processviewer.views.ProcessView.ProcessInfo;


public class ProcessesTableComparator extends ViewerComparator {
  private int propertyIndex;
  private static final int DESCENDING = 1;
  private int direction = DESCENDING;

  public ProcessesTableComparator() {
    this.propertyIndex = 0;
    direction = DESCENDING;
  }

  public int getDirection() {
    return direction == 1 ? SWT.DOWN : SWT.UP;
  }

  public void setColumn(int column) {
    if (column == this.propertyIndex) {
      // Same column as last sort; toggle the direction
      direction = 1 - direction;
    } else {
      // New column; do an ascending sort
      this.propertyIndex = column;
      direction = DESCENDING;
    }
  }

  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
	  ProcessInfo p1 = (ProcessInfo) e1;
	  ProcessInfo p2 = (ProcessInfo) e2;
    int rc = 0;
    switch (propertyIndex) {
    case 0:
      rc = p1.name.compareTo(p2.name);
      break;
    case 1:
    	if( p1.memory < p2.memory)
			rc = 1;
		else if( p1.memory > p2.memory)
			rc = -1;
		else
			rc = 0;
      break;
    case 2:
    	if( p1.cpu < p2.cpu )
			rc = 1;
		else if( p1.cpu > p2.cpu )
			rc = -1;
		else
			rc = 0;
      break;
    default:
      rc = 0;
    }
    // If descending order, flip the direction
    if (direction == DESCENDING) {
      rc = -rc;
    }
    return rc;
  }

} 