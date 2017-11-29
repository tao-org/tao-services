package ro.cs.tao.services.monitoring.impl;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

/**
 * @author Cosmin Cara
 */
public class SnmpClient {
    public static final OID SystemDescription = new OID(".1.3.6.1.2.1.1.1.0");
    public static final OID SystemName = new OID(".1.3.6.1.2.1.1.5.0");
    public static final OID LastMinuteCPULoad = new OID(".1.3.6.1.4.1.2021.10.1.3.1");
    public static final OID PercentageUserCPUTime = new OID(".1.3.6.1.4.1.2021.11.9.0");
    public static final OID TotalMemoryUsed = new OID(".1.3.6.1.4.1.2021.4.6.0");
    public static final OID TotalMemoryFree = new OID(".1.3.6.1.4.1.2021.4.11.0");
    public static final OID ProcessorCount = new OID(".1.3.6.1.2.1.25.3.2");
    public static final OID SystemUpTime = new OID("1.3.6.1.2.1.25.1.1.0");

    private final String address;
    private Snmp snmp;

    public SnmpClient(String hostName) {
        this.address = "udp:" + hostName + "/161";
    }

    public void start() throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();
    }

    public String getAsString(OID oid) throws IOException {
        ResponseEvent event = get(oid);
        return event.getResponse().get(0).getVariable().toString();
    }

    public ResponseEvent get(OID... oids) throws IOException {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GET);
        ResponseEvent event = snmp.send(pdu, getTarget(), null);
        if (event != null) {
            return event;
        }
        throw new IOException("SNMP GET timed out");
    }

    private Target getTarget() {
        Address targetAddress = GenericAddress.parse(this.address);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }
}
