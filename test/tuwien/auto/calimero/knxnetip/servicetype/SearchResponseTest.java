/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018 B. Malinowsky

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    Linking this library statically or dynamically with other modules is
    making a combined work based on this library. Thus, the terms and
    conditions of the GNU General Public License cover the whole
    combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent
    modules, and to copy and distribute the resulting executable under terms
    of your choice, provided that you also meet, for each linked independent
    module, the terms and conditions of the license of that module. An
    independent module is a module which is not derived from or based on
    this library. If you modify this library, you may extend this exception
    to your version of the library, but you are not obligated to do so. If
    you do not wish to do so, delete this exception statement from your
    version.
*/

package tuwien.auto.calimero.knxnetip.servicetype;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.KNXnetIPRouting;
import tuwien.auto.calimero.knxnetip.util.DIB;
import tuwien.auto.calimero.knxnetip.util.DeviceDIB;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.knxnetip.util.ServiceFamiliesDIB;
import tuwien.auto.calimero.knxnetip.util.TunnelingDib;

class SearchResponseTest {
	private final HPAI hpai = new HPAI((InetAddress) null, 0);

	private DeviceDIB device;
	private final ServiceFamiliesDIB svcFamilies = new ServiceFamiliesDIB(new int[] { 2, 3, 4 }, new int[] { 1, 1, 1 });
	private final ServiceFamiliesDIB secureFamilies = ServiceFamiliesDIB.newSecureServiceFamilies(new int[] { 3, 4 }, new int[] { 1, 1 });
	private final TunnelingDib tunneling = new TunnelingDib(List.of(new IndividualAddress(1, 2, 3)), new int[] { 1 });

	@BeforeEach
	void init() throws UnknownHostException {
		device = new DeviceDIB("test", 0, 0, DeviceDIB.MEDIUM_TP1, new IndividualAddress(1234), new byte[6],
				InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST), new byte[6]);
	}

	@Test
	void structLengthOfBasicResponse() {
		final List<DIB> dibs = List.of(device, svcFamilies);
		final SearchResponse res = new SearchResponse(false, hpai, dibs);

		assertEquals(device.getStructLength() + svcFamilies.getStructLength() + tunneling.getStructLength(), res.getStructLength());
	}

	@Test
	void structLengthWithRequestedDibs() {
		final List<DIB> dibs = List.of(device, svcFamilies, secureFamilies, tunneling);
		final SearchResponse res = new SearchResponse(true, hpai, dibs);
		final int expected = hpai.getStructLength() + device.getStructLength() + svcFamilies.getStructLength()
				+ secureFamilies.getStructLength() + tunneling.getStructLength();

		assertEquals(expected, res.getStructLength());
	}

	@Test
	void basicResponseFromByteArray() throws KNXFormatException {
		final List<DIB> dibs = List.of(device, svcFamilies);
		final SearchResponse res = new SearchResponse(false, hpai, dibs);
		final byte[] packet = PacketHelper.toPacket(res);

		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		final SearchResponse parsed = SearchResponse.from(h, packet, h.getStructLength());

		assertEquals(KNXnetIPHeader.SEARCH_RES, parsed.svcType);
		assertEquals(res.getStructLength(), parsed.getStructLength());
		assertEquals(res, parsed);
	}

	@Test
	void responseFromByteArray() throws KNXFormatException {
		final List<DIB> dibs = List.of(device, svcFamilies, secureFamilies, tunneling);

		final SearchResponse res = new SearchResponse(true, hpai, dibs);
		final byte[] packet = PacketHelper.toPacket(res);

		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		final SearchResponse parsed = SearchResponse.from(h, packet, h.getStructLength());

		assertEquals(KNXnetIPHeader.SearchResponse, parsed.svcType);
		assertEquals(res.getStructLength(), parsed.getStructLength());
		assertEquals(res, parsed);
	}

	@Test
	void validSearchResponse() {
		new SearchResponse(hpai, device, svcFamilies);
		new SearchResponse(false, hpai, List.of(device, svcFamilies));
		new SearchResponse(true, hpai, List.of(device, svcFamilies));
	}

	@Test
	void invalidSearchResponse() {
		assertThrows(KNXIllegalArgumentException.class, () -> new SearchResponse(false, hpai, List.of()), "empty DIB list");
		assertThrows(KNXIllegalArgumentException.class, () -> new SearchResponse(true, hpai, List.of()), "empty DIB list");

		assertThrows(KNXIllegalArgumentException.class, () -> new SearchResponse(false, hpai, List.of(device)), "DIB list size < 2");
		assertThrows(KNXIllegalArgumentException.class, () -> new SearchResponse(true, hpai, List.of(device)), "DIB list size < 2");
	}

	@Test
	void testTunnelingDib() throws KNXFormatException {
		assertThrows(KNXFormatException.class, () -> new TunnelingDib(tunneling.toByteArray(), 0, 4), "Dib size < 8");
		assertThrows(KNXFormatException.class, () -> new TunnelingDib(tunneling.toByteArray(), 0, 5), "Dib size < 8");
		assertThrows(KNXFormatException.class, () -> new TunnelingDib(tunneling.toByteArray(), 0, 6), "Dib size < 8");
		assertThrows(KNXFormatException.class, () -> new TunnelingDib(tunneling.toByteArray(), 0, 7), "Dib size < 8");
		assertDoesNotThrow(() -> new TunnelingDib(tunneling.toByteArray(), 0, 8));

		TunnelingDib dib = tunneling;
		byte[] bytes = new byte[]{(byte) 0x08, (byte) 0x07, (byte) 0x00, (byte) 0xfe, (byte) 0x12, (byte) 0x03, (byte) 0xff, (byte) 0xf9};
		assertArrayEquals(bytes, dib.toByteArray());

		dib = new TunnelingDib(List.of(new IndividualAddress(1, 2, 3)), new int[] { 7 });
		bytes = new byte[]{(byte) 0x08, (byte) 0x07, (byte) 0x00, (byte) 0xfe, (byte) 0x12, (byte) 0x03, (byte) 0xff, (byte) 0xff};
		assertArrayEquals(bytes, dib.toByteArray());
	}
}
