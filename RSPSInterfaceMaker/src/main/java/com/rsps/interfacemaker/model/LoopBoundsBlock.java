package com.rsps.interfacemaker.model;

public class LoopBoundsBlock {
    private String groupId = "";
    private String originalText = "";
    private String parentVarName = "";

    public LoopBoundsBlock() {
    }

    public LoopBoundsBlock(String groupId, String originalText, String parentVarName) {
        this.groupId = groupId;
        this.originalText = originalText;
        this.parentVarName = parentVarName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId == null ? "" : groupId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText == null ? "" : originalText;
    }

    public String getParentVarName() {
        return parentVarName;
    }

    public void setParentVarName(String parentVarName) {
        this.parentVarName = parentVarName == null ? "" : parentVarName;
    }
}
