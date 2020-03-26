package org.testfun.jee.runner.inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.commons.lang.StringUtils;

public class MockInitialContextFactory implements InitialContextFactory {

    public static final String SEPARATOR = "/";

    protected static MockContext instance = new MockContext();

    public MockInitialContextFactory() throws NamingException {}

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return instance;
    }

    public static MockContext getMockContext() {
        return instance;
    }

    public static class MockContext implements Context {

        protected Map<String, Object> map = new ConcurrentHashMap<String, Object>();

        protected String toString(Name name) {
            return StringUtils.join(Collections.list(name.getAll()), SEPARATOR);
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            return lookup(toString(name));
        }

        @Override
        public Object lookup(String name) throws NamingException {
            name = normalizeName(name);
            if (!map.containsKey(name)) {
                throw new NamingException("'" + name + "' is not bound");
            }
            return map.get(name);
        }

        @Override
        public void bind(Name name, Object obj) throws NamingException {
            bind(toString(name), obj);
        }

        @Override
        public void bind(String name, Object obj) throws NamingException {
            name = normalizeName(name);
            if (map.containsKey(name)) {
                throw new NamingException("'" + name + "' is already bound");
            }
            rebind(name, obj);
        }

        @Override
        public void rebind(Name name, Object obj) {
            rebind(toString(name), obj);
        }

        @Override
        public void rebind(String name, Object obj) {
            name = normalizeName(name);
            map.put(name, obj);
        }

        @Override
        public void unbind(Name name) {
            unbind(toString(name));
        }

        @Override
        public void unbind(String name) {
            name = normalizeName(name);
            map.remove(name);
        }

        @Override
        public void rename(Name oldName, Name newName) throws NamingException {
            rename(toString(oldName), toString(newName));
        }

        @Override
        public void rename(String oldName, String newName) throws NamingException {
            Object obj = lookup(oldName);
            unbind(oldName);
            rebind(newName, obj);
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            return list(toString(name));
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            name = normalizeName(name);
            List<NameClassPair> bindings = new ArrayList<NameClassPair>();
            name = name + SEPARATOR;
            for (Map.Entry<String, Object> me : map.entrySet()) {
                if (me.getKey().startsWith(name)) {
                    bindings.add(new NameClassPair(me.getKey(), me.getValue().getClass().getName()));
                }
            }
            return (NamingEnumeration<NameClassPair>) Collections.enumeration(bindings);
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            return listBindings(toString(name));
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String name) {
            name = normalizeName(name);
            List<Binding> bindings = new ArrayList<Binding>();
            name = name + SEPARATOR;
            for (Map.Entry<String, Object> me : map.entrySet()) {
                if (me.getKey().startsWith(name)) {
                    bindings.add(new Binding(me.getKey(), me.getValue().getClass().getName(), me.getValue()));
                }
            }
            return (NamingEnumeration<Binding>) Collections.enumeration(bindings);
        }

        @Override
        public void destroySubcontext(Name name) throws NamingException {
            destroySubcontext(toString(name));
        }

        @Override
        public void destroySubcontext(String name) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Context createSubcontext(Name name) throws NamingException {
            return createSubcontext(toString(name));
        }

        @Override
        public Context createSubcontext(String name) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Object lookupLink(Name name) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Object lookupLink(String name) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public NameParser getNameParser(Name name) throws NamingException {
            return getNameParser(toString(name));
        }

        @Override
        public NameParser getNameParser(String name) throws NamingException {
            return new MockNameParser(name);
        }

        @Override
        public Name composeName(Name name, Name prefix) throws NamingException {
            return new MockName(prefix).addAll(name);
        }

        @Override
        public String composeName(String name, String prefix) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Object removeFromEnvironment(String propName) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public void close() throws NamingException {}

        @Override
        public String getNameInNamespace() throws NamingException {
            throw new Error("not implemented");
        }

        // Other
        public void put(Class<?> cls, Object obj) {
            map.put("mock:" + cls.getName(), obj);
        }

        public Object get(Class<?> cls) {
            return map.get("mock:" + cls.getName());
        }

        public void put(String key, Object obj) {
            map.put(key, obj);
        }

        public Object get(String key) {
            return map.get(key);
        }

        public void clear() {
            Iterator<String> it = map.keySet().iterator();
            while (it.hasNext()) {
                if (it.next().startsWith("mock:")) {
                    it.remove();
                }
            }
        }

        public boolean contains(String key) {
            return map.containsKey(key);
        }

        private String normalizeName(String name) {
            return name.replaceAll("^/", "");
        }

    }

    private static class MockNameParser implements NameParser {

        private Name root;

        public MockNameParser(String name) {
            this.root = new MockName(name);
        }

        @Override
        public Name parse(String name) throws NamingException {
            return ((Name) root.clone()).addAll(new MockName(name));
        }
    }

    private static class MockName implements Name {

        private static final long serialVersionUID = 1L;
        private final List<String> elems = new ArrayList<String>();

        public MockName(List<String> subList) {
            elems.addAll(subList);
        }

        public MockName(Name prefix) {
            addAll(prefix);
        }

        public MockName(String name) {
            elems.addAll(Arrays.asList(name.split(SEPARATOR)));
        }

        @Override
        public MockName clone() {
            return new MockName(elems);
        }

        @Override
        public int compareTo(Object obj) {
            if (equals(obj)) {
                return 0;
            }
            throw new Error("not implemented");
        }

        @Override
        public int size() {
            return elems.size();
        }

        @Override
        public boolean isEmpty() {
            return elems.isEmpty();
        }

        @Override
        public Enumeration<String> getAll() {
            return Collections.enumeration(elems);
        }

        @Override
        public String get(int posn) {
            return elems.get(posn);
        }

        @Override
        public Name getPrefix(int posn) {
            return new MockName(elems.subList(0, posn - 1));
        }

        @Override
        public Name getSuffix(int posn) {
            return new MockName(elems.subList(posn, elems.size()));
        }

        @Override
        public boolean startsWith(Name n) {
            return getPrefix(n.size()).equals(n);
        }

        @Override
        public boolean endsWith(Name n) {
            return getSuffix(elems.size() - n.size()).equals(n);
        }

        @Override
        public Name addAll(Name suffix) {
            elems.addAll(Collections.list(suffix.getAll()));
            return this;
        }

        @Override
        public Name addAll(int posn, Name n) {
            elems.addAll(posn, Collections.list(n.getAll()));
            return this;
        }

        @Override
        public Name add(String comp) {
            elems.add(comp);
            return this;
        }

        @Override
        public Name add(int posn, String comp) throws InvalidNameException {
            elems.add(posn, comp);
            return this;
        }

        @Override
        public Object remove(int posn) throws InvalidNameException {
            return elems.remove(posn);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((elems == null) ? 0 : elems.hashCode());
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
            MockName other = (MockName) obj;
            if (elems == null) {
                if (other.elems != null)
                    return false;
            } else if (!elems.equals(other.elems))
                return false;
            return true;
        }
    }

}
