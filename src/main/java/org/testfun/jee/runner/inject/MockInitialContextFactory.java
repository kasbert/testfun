package org.testfun.jee.runner.inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.commons.lang.StringUtils;

public class MockInitialContextFactory implements InitialContextFactory {

    protected static MockContext instance = new MockContext();

    public MockInitialContextFactory() throws NamingException {
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return instance;
    }

    public static MockContext getMockContext() {
        return instance;
    }

    public static class MockContext implements Context {

        protected Map<String, Object> map = new ConcurrentHashMap<String, Object>();

        public static final String SEPARATOR = "/";

        protected String toString(Name name) {
            return StringUtils.join(Collections.list(name.getAll()), SEPARATOR);
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            return lookup(toString(name));
        }

        @Override
        public Object lookup(String name) throws NamingException {
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
            if (map.containsKey(name)) {
                throw new NamingException("'" + name + "' is already bound");
            }
            rebind(name, obj);
        }

        @Override
        public void rebind(Name name, Object obj) throws NamingException {
            rebind(toString(name), obj);
        }

        @Override
        public void rebind(String name, Object obj) throws NamingException {
            map.put(name, obj);
        }

        @Override
        public void unbind(Name name) throws NamingException {
            unbind(toString(name));
        }

        @Override
        public void unbind(String name) throws NamingException {
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
        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
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
            throw new Error("not implemented");
        }

        @Override
        public NameParser getNameParser(String name) throws NamingException {
            throw new Error("not implemented");
        }

        @Override
        public Name composeName(Name name, Name prefix) throws NamingException {
            throw new Error("not implemented");
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
        public void close() throws NamingException {
        }

        @Override
        public String getNameInNamespace() throws NamingException {
            throw new Error("not implemented");
        }

        //
        public void put(Class<?> cls, Object obj) {
            map.put("mock:" + cls.getName(), obj);
        }

        public Object get(Class<?> cls) {
            return map.get("mock:" + cls.getName());
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

    }

}
