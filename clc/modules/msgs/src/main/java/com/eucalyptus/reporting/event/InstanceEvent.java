package com.eucalyptus.reporting.event;

/**
 * <p>InstanceEvent is an event sent from the CLC to the reporting mechanism,
 * indicating resource usage by an instance.
 * 
 * <p>InstanceEvent contains the <i>cumulative</i> usage of an instance up until
 * this point. For example, it contains all network and disk bandwidth used up
 * until the point when the InstanceEvent was instantiated. This is different
 * from data returned by the reporting mechanism, which contains only usage data
 * for a specific period.
 * 
 * <p>By using cumulative usage totals, we gain resilience to lost packets
 * despite unreliable transmission. If an event is lost, then the next event
 * will contain the cumulative usage and nothing will be lost, and the sampling
 * period will be assumed to have begun at the end of the last successfully
 * received event. As a result, lost packets cause only loss of granularity of
 * time periods, but no loss of usage information.
 * 
 * <p>InstanceEvent allows null values for usage statistics like
 * cumulativeDiskIo. Null values signify missing information and not zero
 * usage. Null values will be ignored while calculating aggregate usage
 * information for reports. Null values should be used only when we don't
 * support gathering information from an instance <i>at all</i>. Null values
 * for resource usage will be represented as "N/A" or something similar in
 * UIs.
 * 
 * @author tom.werges
 */
@SuppressWarnings("serial")
public class InstanceEvent
	implements Event
{
	private String uuid;
	private String instanceId;
	private String instanceType;
	private String userId;
	private String userName;
	private String accountId;
	private String accountName;
	private String clusterName;
	private String availabilityZone;
	private Long   cumulativeNetworkIoMegs;
	private Long   cumulativeDiskIoMegs;
	

	/**
 	 * Constructor for InstanceEvent. 
 	 *
 	 * NOTE: We must include separate userId, username, accountId, and
 	 *  accountName with each event sent, even though the names can be looked
 	 *  up using ID's. We must include this redundant information, for
 	 *  several reasons. First, the reporting subsystem may run on a totally
 	 *  separate machine outside of eucalyptus (data warehouse configuration)
 	 *  so it may not have access to the regular eucalyptus database to lookup
 	 *  usernames or account names. Second, the reporting subsystem stores
 	 *  <b>historical</b> information, and its possible that usernames and
 	 *  account names can change, or their users or accounts can be deleted.
 	 *  Thus we need the user name or account name at the time an event was
 	 *  sent.
 	 */
	public InstanceEvent(String uuid, String instanceId, String instanceType,
			String userId, String userName, String accountId,
			String accountName, String clusterName,	String availabilityZone,
			Long cumulativeNetworkIoMegs, Long cumulativeDiskIoMegs)
	{
		if (uuid==null) throw new IllegalArgumentException("uuid cant be null");
		if (instanceId==null) throw new IllegalArgumentException("instanceId cant be null");
		if (instanceType==null) throw new IllegalArgumentException("instanceType cant be null");
		if (userId==null) throw new IllegalArgumentException("userId cant be null");
		if (userName==null) throw new IllegalArgumentException("userName cant be null");
		if (accountId==null) throw new IllegalArgumentException("accountId cant be null");
		if (accountName==null) throw new IllegalArgumentException("accountName cant be null");
		if (clusterName==null) throw new IllegalArgumentException("clusterName cant be null");
		if (availabilityZone==null) throw new IllegalArgumentException("availabilityZone cant be null");
		if (cumulativeDiskIoMegs!= null && cumulativeDiskIoMegs.longValue() < 0) {
			throw new IllegalArgumentException("cumulativeDiskIoMegs cant be negative");
		}
		if (cumulativeNetworkIoMegs!= null && cumulativeNetworkIoMegs.longValue() < 0) {
			throw new IllegalArgumentException("cumulativeNetworkIoMegs cant be negative");
		}
		
		this.uuid = uuid;
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.userId = userId;
		this.userName = userName;
		this.accountId = accountId;
		this.accountName = accountName;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
		this.cumulativeNetworkIoMegs = cumulativeNetworkIoMegs;
		this.cumulativeDiskIoMegs = cumulativeDiskIoMegs;
	}
	

	/**
 	 * Copy Constructor.
 	 *
 	 * NOTE: We must include separate userId, username, accountId, and
 	 *  accountName with each event sent, even though the names can be looked
 	 *  up using ID's. We must include this redundant information, for
 	 *  several reasons. First, the reporting subsystem may run on a totally
 	 *  separate machine outside of eucalyptus (data warehouse configuration)
 	 *  so it may not have access to the regular eucalyptus database to lookup
 	 *  usernames or account names. Second, the reporting subsystem stores
 	 *  <b>historical</b> information, and its possible that usernames and
 	 *  account names can change, or their users or accounts can be deleted.
 	 *  Thus we need the user name or account name at the time an event was
 	 *  sent.
 	 */
	public InstanceEvent(InstanceEvent e)
	{
		this.uuid = e.uuid;
		this.instanceId = e.instanceId;
		this.instanceType = e.instanceType;
		this.userId = e.userId;
		this.userName = e.userName;
		this.accountId = e.accountId;
		this.accountName = e.accountName;
		this.clusterName = e.clusterName;
		this.availabilityZone = e.availabilityZone;
		this.cumulativeNetworkIoMegs = e.cumulativeNetworkIoMegs;
		this.cumulativeDiskIoMegs = e.cumulativeDiskIoMegs;		
	}

	/**
	 * Copy constructor with different resource usage
	 */
	public InstanceEvent(InstanceEvent e, Long cumulativeNetworkIoMegs, Long cumulativeDiskIoMegs)
	{
		if (cumulativeDiskIoMegs!= null && cumulativeDiskIoMegs.longValue() < 0) {
			throw new IllegalArgumentException("cumulativeDiskIoMegs cant be negative");
		}
		if (cumulativeNetworkIoMegs!= null && cumulativeNetworkIoMegs.longValue() < 0) {
			throw new IllegalArgumentException("cumulativeNetworkIoMegs cant be negative");
		}

		this.uuid = e.uuid;
		this.instanceId = e.instanceId;
		this.instanceType = e.instanceType;
		this.userId = e.userId;
		this.userName = e.userName;
		this.accountId = e.accountId;
		this.accountName = e.accountName;
		this.clusterName = e.clusterName;
		this.availabilityZone = e.availabilityZone;
		this.cumulativeNetworkIoMegs = cumulativeNetworkIoMegs;
		this.cumulativeDiskIoMegs = cumulativeDiskIoMegs;		
	}

	public String getUuid()
	{
		return uuid;
	}

	public String getInstanceId()
	{
		return instanceId;
	}

	public String getInstanceType()
	{
		return instanceType;
	}

	public String getUserId()
	{
		return userId;
	}
	
	public String getUserName()
	{
		return userName;
	}

	public String getAccountId()
	{
		return accountId;
	}
	
	public String getAccountName()
	{
		return accountName;
	}
	
	public String getClusterName()
	{
		return clusterName;
	}

	public String getAvailabilityZone()
	{
		return availabilityZone;
	}

	public Long getCumulativeNetworkIoMegs()
	{
		return cumulativeNetworkIoMegs;
	}

	public Long getCumulativeDiskIoMegs()
	{
		return cumulativeDiskIoMegs;
	}

	public boolean requiresReliableTransmission()
	{
		return false;
	}
	
	public String toString()
	{
		return String.format("[uuid:%s,instanceId:%s,instanceType:%s,userId:%s"
				+ ",accountId:%s,cluster:%s,zone:%s,net:%d,disk:%d]",
					uuid, instanceId, instanceType, userId, accountId,
					clusterName, availabilityZone, cumulativeNetworkIoMegs,
					cumulativeDiskIoMegs);
	}

}
