package net.taviscaron.dcpu16vm.config;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Instantiate machine from xml configuration
 *
 * Example:

 * <machine type="net.taviscaron.dcpu16vm.machine.impl.MachineImpl">
 *     <processor type="net.taviscaron.dcpu16vm.machine.impl.ProcessorImpl"/>
 *     <memory type="net.taviscaron.dcpu16vm.machine.impl.MemoryImpl"/>
 *     <hardware>
 *         <device type="net.taviscaron.dcpu16vm.device.GenericClock"/>
 *         <device type="net.taviscaron.dcpu16vm.device.GenericKeyboard"/>
 *         <device type="net.taviscaron.dcpu16vm.device.LEM1802"/>
 *     </hardware>
 * </machine>
 *
 * @author Andrei Senchuk
 */
public class XmlMachineBuilder implements MachineBuilder {
    private enum Tag {
        MACHINE, PROCESSOR, MEMORY, HARDWARE, DEVICE
    }

    private class XmlConfigParser extends DefaultHandler {
        private boolean hwConfig;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch(Tag.valueOf(qName.toUpperCase())) {
                case MACHINE:
                    if(!hwConfig) {
                        machineClassName = getType(qName, attributes);
                    } else {
                        throw new SAXException("machine configuration can't be placed in hardware section");
                    }
                    break;
                case PROCESSOR:
                    if(!hwConfig) {
                        processorClassName = getType(qName, attributes);
                    } else {
                        throw new SAXException("processor configuration can't be placed in hardware section");
                    }
                    break;
                case MEMORY:
                    if(!hwConfig) {
                        memoryClassName = getType(qName, attributes);
                    } else {
                        throw new SAXException("memory configuration can't be placed in hardware section");
                    }
                    break;
                case HARDWARE:
                    hwConfig = true;
                    break;
                case DEVICE:
                    if(hwConfig) {
                        deviceClassesNames.add(getType(qName, attributes));
                    } else {
                        throw new SAXException("device configuration should be placed in hardware section");
                    }
                    break;
                default:
                    throw new SAXException("Unknown node: " + qName);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch(Tag.valueOf(qName.toUpperCase())) {
                case HARDWARE:
                    hwConfig = false;
                    break;
            }
        }

        private String getType(String tag, Attributes attributes) throws SAXException {
            String type = attributes.getValue("type");
            if(type == null) {
                throw new SAXException("type attribute is missing for tag: " + tag);
            }
            return type;
        }
    }

    private String machineClassName;
    private String memoryClassName;
    private String processorClassName;
    private List<String> deviceClassesNames = new LinkedList<String>();

    public XmlMachineBuilder(String file) throws InvalidConfigurationException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new File(file), new XmlConfigParser());
        } catch(ParserConfigurationException e) {
            throw new RuntimeException("Parser was wrong configured", e);
        } catch(SAXException e) {
            throw new InvalidConfigurationException("Configuration is invalid: " + e.getMessage(), e);
        } catch(IOException e) {
            throw new InvalidConfigurationException("Configuration read error" +  e.getMessage(), e);
        }
    }

    @Override
    public Machine createMachine() throws InvalidConfigurationException {
        Machine machine = null;
        try {
            machine = (Machine)instantiateClass(machineClassName);
        } catch(ClassCastException e) {
            throw new InvalidConfigurationException("Specified machine type " + machineClassName + " is not actually a machine");
        } catch(IllegalArgumentException e) {
            throw new InvalidConfigurationException("Machine  is not configured");
        }

        try {
            machine.setProcessor((Processor)instantiateClass(processorClassName));
        } catch(ClassCastException e) {
            throw new InvalidConfigurationException("Specified processor type " + processorClassName + " is not actually a processor");
        } catch(IllegalArgumentException e) {
            throw new InvalidConfigurationException("Processor is not configured");
        }

        try {
            machine.setMemory((Memory)instantiateClass(memoryClassName));
        } catch(ClassCastException e) {
            throw new InvalidConfigurationException("Specified memory type " + memoryClassName + " is not actually a memory");
        } catch(IllegalArgumentException e) {
            throw new InvalidConfigurationException("Memory is not configured");
        }

        List<Device> devicesList = new ArrayList<Device>(deviceClassesNames.size());
        for(String name : deviceClassesNames) {
            try {
                devicesList.add((Device)instantiateClass(name));
            } catch(ClassCastException e) {
                throw new InvalidConfigurationException("Specified device type " + name + " is not actually a device");
            } catch(IllegalArgumentException e) {
                throw new InvalidConfigurationException("Device is not configured");
            }
        }

        Device[] devices = new Device[devicesList.size()];
        devicesList.toArray(devices);
        machine.setDevices(devices);

        return machine;
    }

    private Object instantiateClass(String name) throws InvalidConfigurationException {
        try {
            if(name != null) {
                Class clazz = Class.forName(name);
                return clazz.newInstance();
            } else {
                throw new IllegalArgumentException();
            }
        } catch (ClassNotFoundException e) {
            throw new InvalidConfigurationException("Can't load class " + name, e);
        } catch (InstantiationException e) {
            throw new InvalidConfigurationException("Can't create an instance of the class " + name, e);
        } catch (IllegalAccessException e) {
            throw new InvalidConfigurationException("Can't create an instance of the class " + name, e);
        }
    }
}
