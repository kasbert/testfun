package org.testfun.jee;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;

@Table(uniqueConstraints=
    @UniqueConstraint(columnNames={"NAME"})
)
@Entity
public class Duplicates {

    @Id
    @GeneratedValue
    @XmlTransient
    private int id;

    private String name;

    @Transient
    private String duplicateName;

    public Duplicates(String name) {
        this.name = name;
    }

    @PreUpdate
    public void callback() {
        duplicateName = name;
    }

    public Duplicates() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDuplicateName() {
        return duplicateName;
    }

    public void setDuplicateName(String duplicateName) {
        this.duplicateName = duplicateName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((duplicateName == null) ? 0 : duplicateName.hashCode());
        result = prime * result + id;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Duplicates other = (Duplicates) obj;
        if (duplicateName == null) {
            if (other.duplicateName != null)
                return false;
        } else if (!duplicateName.equals(other.duplicateName))
            return false;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
