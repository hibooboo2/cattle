/**
 * This class is generated by jOOQ
 */
package io.github.ibuildthecloud.dstack.core.tables;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = { "http://www.jooq.org", "3.2.0" },
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class VnetTable extends org.jooq.impl.TableImpl<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord> {

	private static final long serialVersionUID = 332833064;

	/**
	 * The singleton instance of <code>dstack.vnet</code>
	 */
	public static final io.github.ibuildthecloud.dstack.core.tables.VnetTable VNET = new io.github.ibuildthecloud.dstack.core.tables.VnetTable();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord> getRecordType() {
		return io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord.class;
	}

	/**
	 * The column <code>dstack.vnet.id</code>. 
	 */
	public final org.jooq.TableField<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord, java.lang.Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this);

	/**
	 * The column <code>dstack.vnet.uri</code>. 
	 */
	public final org.jooq.TableField<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord, java.lang.String> URI = createField("uri", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this);

	/**
	 * The column <code>dstack.vnet.scope</code>. 
	 */
	public final org.jooq.TableField<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord, java.lang.String> SCOPE = createField("scope", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false).defaulted(true), this);

	/**
	 * The column <code>dstack.vnet.ip_pool_qualifier</code>. 
	 */
	public final org.jooq.TableField<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord, java.lang.String> IP_POOL_QUALIFIER = createField("ip_pool_qualifier", org.jooq.impl.SQLDataType.VARCHAR.length(128), this);

	/**
	 * The column <code>dstack.vnet.ip_pool_segment</code>. 
	 */
	public final org.jooq.TableField<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord, java.lang.String> IP_POOL_SEGMENT = createField("ip_pool_segment", org.jooq.impl.SQLDataType.VARCHAR.length(128), this);

	/**
	 * Create a <code>dstack.vnet</code> table reference
	 */
	public VnetTable() {
		super("vnet", io.github.ibuildthecloud.dstack.core.DstackTable.DSTACK);
	}

	/**
	 * Create an aliased <code>dstack.vnet</code> table reference
	 */
	public VnetTable(java.lang.String alias) {
		super(alias, io.github.ibuildthecloud.dstack.core.DstackTable.DSTACK, io.github.ibuildthecloud.dstack.core.tables.VnetTable.VNET);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord, java.lang.Long> getIdentity() {
		return io.github.ibuildthecloud.dstack.core.Keys.IDENTITY_VNET;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord> getPrimaryKey() {
		return io.github.ibuildthecloud.dstack.core.Keys.KEY_VNET_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<io.github.ibuildthecloud.dstack.core.tables.records.VnetRecord>>asList(io.github.ibuildthecloud.dstack.core.Keys.KEY_VNET_PRIMARY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public io.github.ibuildthecloud.dstack.core.tables.VnetTable as(java.lang.String alias) {
		return new io.github.ibuildthecloud.dstack.core.tables.VnetTable(alias);
	}
}