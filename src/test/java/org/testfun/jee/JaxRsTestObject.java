package org.testfun.jee;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class JaxRsTestObject {

    private String str;
    private int num;

    public JaxRsTestObject() {
    }

    public JaxRsTestObject(String str, int num) {
        super();
        this.str = str;
        this.num = num;
    }

    public String getStr() {
        return str;
    }
    public void setStr(String str) {
        this.str = str;
    }
    public int getNum() {
        return num;
    }
    public void setNum(int num) {
        this.num = num;
    }

}
