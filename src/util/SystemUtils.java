package util;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class SystemUtils {

	
	
	public static double getProcessCpuLoad() {
		AttributeList list = null;
		Double value = 0.0;

		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

			if (list.isEmpty()) {
				return 0;
			}

			Attribute att = (Attribute) list.get(0);
			value = (Double) att.getValue();
			if (value == -1.0) {
				return 0;
			} // usually takes a couple of seconds before we get real values

		} catch (MalformedObjectNameException | InstanceNotFoundException| ReflectionException e) {
			e.printStackTrace();
		}

		return ((int) (value * 1000) / 10.0); // returns a percentage value with
												// 1 decimal point precision
	}
	
	public static double getProcessCpuLoad2() {
		com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		return round(bean.getSystemCpuLoad(), 2);
	}

	// RESTRICTED method. Follow this link to solve trouble
	// http://stackoverflow.com/questions/860187/access-restriction-on-class-due-to-restriction-on-required-library-rt-jar
	public static long getMaxRAMAmount() {
		com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		long maxRam = bean.getTotalPhysicalMemorySize();
		return maxRam;
	}

	public static long getFreeRAMAmount() {
		com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		long freeRam = bean.getFreePhysicalMemorySize();
		return freeRam;
	}

	public static double getUsagesMemoryInPercent() {
		double mb = 1024 * 1024;

		double memoryUsed = (getMaxRAMAmount() - getFreeRAMAmount()) / mb;
		double maxRam = getMaxRAMAmount() / mb;
		double result = ((100 * memoryUsed / maxRam) / 100);
		result = round(result, 2);
		return result;
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

}
