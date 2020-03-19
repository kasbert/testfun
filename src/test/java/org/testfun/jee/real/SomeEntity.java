package org.testfun.jee.real;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Length.List;

import javax.persistence.*;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Entity
public class SomeEntity {

    @Id
    @GeneratedValue
    private int id;

    @Length.List({
            @Length(min = 4, message = "The name must be at least 4 characters"),
            @Length(max = 20, message = "The name must be less than 20 characters")
    })
    private String name;

    private String vcdApiAddress;

    public SomeEntity() {
    }

    public SomeEntity(int id,
            @List({ @Length(min = 4, message = "The name must be at least 4 characters"),
                    @Length(max = 20, message = "The name must be less than 20 characters") }) String name,
            String vcdApiAddress) {
        super();
        this.id = id;
        this.name = name;
        this.vcdApiAddress = vcdApiAddress;
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

    public String getVcdApiAddress() {
        return vcdApiAddress;
    }

    public void setVcdApiAddress(String vcdApiAddress) {
        this.vcdApiAddress = vcdApiAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((vcdApiAddress == null) ? 0 : vcdApiAddress.hashCode());
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
        SomeEntity other = (SomeEntity) obj;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (vcdApiAddress == null) {
            if (other.vcdApiAddress != null)
                return false;
        } else if (!vcdApiAddress.equals(other.vcdApiAddress))
            return false;
        return true;
    }

}
