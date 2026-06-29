package com.firesafety.platform.organization;

public class Enterprise {
    private Long id;
    private final Long parentId;
    private final String name;
    private final String contactName;
    private final String contactPhone;
    private boolean enabled;

    private Enterprise(Long parentId, String name, String contactName, String contactPhone) {
        this.parentId = parentId;
        this.name = name;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.enabled = true;
    }

    public static Enterprise headquarters(String name, String contactName, String contactPhone) {
        return new Enterprise(null, name, contactName, contactPhone);
    }

    public static Enterprise branch(Long parentId, String name, String contactName, String contactPhone) {
        if (parentId == null) {
            throw new IllegalArgumentException("子企业必须指定上级企业");
        }
        return new Enterprise(parentId, name, contactName, contactPhone);
    }

    public static Enterprise restore(
            Long id, Long parentId, String name, String contactName, String contactPhone, boolean enabled) {
        var enterprise = new Enterprise(parentId, name, contactName, contactPhone);
        enterprise.id = id;
        enterprise.enabled = enabled;
        return enterprise;
    }

    public void assignId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("企业编号已经分配");
        }
        this.id = id;
    }

    public Long id() { return id; }
    public Long parentId() { return parentId; }
    public String name() { return name; }
    public String contactName() { return contactName; }
    public String contactPhone() { return contactPhone; }
    public boolean enabled() { return enabled; }
    public void disable() { enabled = false; }
}
