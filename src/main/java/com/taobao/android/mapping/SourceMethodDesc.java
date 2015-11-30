package com.taobao.android.mapping;

import java.util.Arrays;

/**
 * Created by vliux on 15-11-26.
 */
public class SourceMethodDesc {
    private String name;
    private String[] params;

    public SourceMethodDesc(String name, String[] params) {
        this.name = name;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public String[] getParams() {
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SourceMethodDesc that = (SourceMethodDesc) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(params, that.params);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (params != null ? Arrays.hashCode(params) : 0);
        return result;
    }
}
