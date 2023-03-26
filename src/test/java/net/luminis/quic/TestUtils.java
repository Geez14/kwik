/*
 * Copyright © 2020, 2021, 2022, 2023 Peter Doornbosch
 *
 * This file is part of Kwik, an implementation of the QUIC protocol in Java.
 *
 * Kwik is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Kwik is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.luminis.quic;

import net.luminis.quic.crypto.Aes128Gcm;
import net.luminis.quic.crypto.Aead;
import net.luminis.quic.crypto.BaseAeadImpl;
import net.luminis.quic.log.Logger;
import net.luminis.tls.util.ByteUtils;
import net.luminis.quic.test.FieldSetter;

import javax.crypto.Cipher;

import static net.luminis.quic.Version.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    /**
     * Create a valid Keys object that can be used for encrypting/decrypting packets in tests.
     * @return
     * @throws Exception
     */
    public static Aead createKeys() throws Exception {
        Aes128Gcm keys = mock(Aes128Gcm.class);
        when(keys.getHp()).thenReturn(new byte[16]);
        when(keys.getWriteIV()).thenReturn(new byte[12]);
        when(keys.getWriteKey()).thenReturn(new byte[16]);
        Aes128Gcm dummyKeys = new Aes128Gcm(Version.getDefault(), new byte[16], null, mock(Logger.class));
        FieldSetter.setField(dummyKeys, BaseAeadImpl.class.getDeclaredField("hp"), new byte[16]);
        Cipher hpCipher = dummyKeys.getHeaderProtectionCipher();
        when(keys.getHeaderProtectionCipher()).thenReturn(hpCipher);
        FieldSetter.setField(dummyKeys, BaseAeadImpl.class.getDeclaredField("writeKey"), new byte[16]);
        Cipher wCipher = dummyKeys.getWriteCipher();
        // The Java implementation of this cipher (GCM), prevents re-use with the same iv.
        // As various tests often use the same packet numbers (used for creating the nonce), the cipher must be re-initialized for each test.
        // Still, a consequence is that generatePacketBytes cannot be called twice on the same packet.
        when(keys.getWriteCipher()).thenReturn(wCipher);
        when(keys.getWriteKeySpec()).thenReturn(dummyKeys.getWriteKeySpec());

        when(keys.aeadEncrypt(any(), any(), any())).thenCallRealMethod();
        when(keys.aeadDecrypt(any(), any(), any())).thenCallRealMethod();
        when(keys.createHeaderProtectionMask(any())).thenCallRealMethod();

        return keys;
    }

    public static byte[] createValidInitial(Version quicVersion) {
        if (quicVersion.equals(IETF_draft_29)) {
            // ODCID: 67268378ae7dc13b   Packet length: 1200
            return ByteUtils.hexToBytes("cbff00001d0867268378ae7dc13b08d5aabb03c53eed7d0044966ba935ce8c9b5516c82781494d098f254ebecc3d2fc3c00b5f4a6f84ae435556be3b47443761a3ba8d454611fd4f8daa1206919c11a8c749cc1250c0d4642fa879f396fee00b9bb55faf81e712628348d6d99df13f3006a19bbc4a3100a4dc9ea0089bf9c9c50ede7d13c13d5c4cd0db4ba0b26c98ebb35beaf46950e7501db32db57f0f4b2c8e8905c8db9c884ad34e810221052ca4bad7385c11c6f99f6f7297b55b5ea032396c1207d97e66f2a11c6f4c0b8dcb6e7a337cb20a72893d3eadd571cf18a6dfc9985463ea26f02ab0d89b2401101bcfe334a00c13aafeb1ff5e78199ccec1e5e5fff747dff32227ae844abb6df9f6f383f8aa0fcfa6620212a53dc9693a9423ec1c8530773af6d403ebe650ed1c3496660be26acc4d7259f78db79e13c4aa8e886270d5da425fa8bbb4d6eecf6dbd95b7de021242b6194b6ad3659badc164284744c41e328681d632258bd8df0378f37ff1d8d98dc7523c185ed15bf1a18fe6e6e63e977a58c4a97ceb5ea4558f8f3242f0ef135300ca5e035c1576e164394ae8158114cd243db197e3471c4126006c80512d80052a1e5abefc89e4e67eeab86a684176ccc610c94b224c228c3cb38eba7890e4b983ed909352fba349dc0d994c4984a7c5acb0f34d6f872f18232f21f70f9d6328aa5a2db058b12b73a7fbee815235063db9960ab2ee65dd3924c6109d85eb825192a2ee16bb5c1fa14c0279d0b4d55a5414854a35a6003c94ff4e3a13b728df2a35b739e64ca070997db4319bcf414ae677e6a362177b3aae8bdebd652283b87ad2985cf0d20636561321c4e64e58eae9e723b02a880ce6d50ff180aca94c26fe092a5d94578c0283e90d7ad3901e25fa4e29fa88212eff3196e93bea9e8fc466a5cd58c8621fe303abffd669624d025787b674f38e80f07fb3cb6a27dcc2ee1ca2ba42944dcf3b9485e12bc3946bb3ac8a5a1765203e5949009cb04137b008dfc09f45f43b7bb21d96a0120600aa565ef074ae80b95e99dec947d948ce64066525906e5d2d9dceab57e4a76b4967c81399cabb28c3d119a7464e1e489c7e40a972ef4676155163924c85a1d52756f4b4018d218774e56e007d7e954675eb40ecad5745eeb8bc8c6dafa898dc6142d3997b5735418c2b9b81e1d70398ae0788d4a936775583b62ef777566f6e9ea71c6848e7aee12baf4c454fc3f57ffd2915aa0d9ea64d8d360ec86bcac5f8c8c5e793cad593d63d26283c32ea206ba3f4407865f0d0a6c7aa1d488b9ce0d32fd7d6469885422ccf228659b8ee566cda76520127b5395b6e7b911c8b0cdddc1d3e9dcaf08efdf1bf40c938b3f08d95d8974069575ddc3b81fef44de347b988cf6bdc33498192b70c2daf0c6ef43653ac96b7c95a2ffb3878985926ae19df2a6bfda342ce2622800fc1cce5a4b319bae73faa7455bd1a2b29df030b8bee6cac8d7a98cf20b6bb288604a0733a215a2bd395a781109cc0017bee36a2717f984ebaf09e9c788740a9fdab28cf66a7f84942c0ef49da17689697e7e6275577a2a22e9d0ba3e03bd56090f43a19493d6e997fc2678403174af27cbae35c113e6e74e583be8c34c03592fb61433ec573da50056ab707a86b293c9710394be5475d4bec3c087f8316b90151b894397d1475");
        }
        else if (quicVersion.equals(QUIC_version_1)) {
            return ByteUtils.hexToBytes("cb0000000108136c48a42e895aaa083020bd5f0c60edb6004496a262b8211c70e8662ee39e52609db716705e38ce63b3fd80862117500b6ae639a6e7bf75bdc268cbfc778fc6ccb07f7849b0fa61d145d30faf5027d90ddf8a95a913b6b348af498301af73ad68d2ef493722e6c2064491ac1807b515deb65f1186dccc1a94da6d1301064c9eda47a0163fd388c28ce7897f4b0b03435122e322b981494e3e3389a67646a0e25cf0ed90bc149eb13e7fa229a568bbe3c630c88e88ea84c20160934eff117d6fd0c3aca03004e9d115e8026ae40a19c93ce5bf8c321656d43856a66bac5cd345bf7853a4bb2b3bc22b251f47c7b8609323c88d6dafa0d9d0f35518c66e6a8e1a77df0fa134c91ae0301dac90bfd2a96a2d6338310f74b7070209f41861e868bb2a82e2fde8b7825eebd8fb34b81cebd3208e034279fe32b8b6cec2c7f9a79403dbee1d4e4389f93f48e5b0c5139c0e1cd2a0754ae21c99c40a2c4e6c69b5f8c13e660ac6b5732d69e7b5593955c70f69a9e4c985c6b0f64d4af68e0d0a7291b89a1b846849f7a86346a57fcb0016a0d1f7a0fd176911222de38601d5a4e8bf2ae702418ee1aedb1aacca1cde034a1c23fd6b526ef767a6077ecc4e465987c294bcc28d386107eda1c0ace4519efec6ee72ab2d3215db090d3fd01052ca5adeded3c4ea900773b5afdaf8f7ba983e150375070e47ece30a9ec9402235d2866ae06bc467d741bace83ba42ea0bf8d946e71a66233886c1ca795a380f2ff1663b9598a1ead3c1d7e092aeed7b6ee1a9e1c4428ae53c0904d19e7bcca053bdfbd84e524ebec1f48d346af441b5cf70ecd309d2dd519fa7933420f2b3cc70828f168c2774f62382dc671b460ea5666a766fcd6ae98ede820e7afa21a987e3d2d5eec0832c28e0bf3b4bc2aa3eb05d3b8cfceac20bfdd4be5b1788e8005dfc9322fb897db1ec9faceb307c23952fbca862ca2fec925dfcda6c2e27066f027e140bf8fea1bdc7a4f6a86c78706dd8d7159af6c63291c548383db5940cb6a19047c31bbcdc765ebc5dcfbb2e2d899f43f33d41b5a931bf3ec74cf410f9919eb4a5af0ca2ccc25d294530b74a893ee97685949a3be9fbd10c222e0650a4307dce096db3679534874896c6b935313367b7579dd8771b08cf0ff702c549da89822ba6e516317f5ea67bcff55c76b301a8614dc8854d0f11ad262932142424c976dd41b4227e61a974d52a7f5c5e8ccf297463a6ebe32983a91ec365db34bb80365782f60fedfd2c5c77ea076203e7771d90cd8934cb64f5fda2d483f0a4b3c4f96bdf5cf75732f61dc96b46157ad37a2fb8ab05f7049d1d78780533b86094b46c58957c58a5a53dd2ad3188f3975de8739e967341429ffe59d3526f2b8f95b42fd09afdae5a3f15b6024504218dd1443aa44706c4d7e5b08c1f68cf610e3060301e789e79ca46e0f99fc6358d5ef635e7cd8683dfb024b3d87c7a632b069218cf5846c979b49d3f4f7a4b947c87b7e610d326cf7f1d0d36e3d0b52c1622645ca9971ce36bc5ea25d4185d5c478f923396b09a529e766f3d3e19c206ed6858b44341c18d61097d1a2086dc5f382cefe6a8a30fc3924f305dedfe2bef6713884d6acb73e569f398bcb39a5bb935f062c729fad7ee5e45356692647cf29cb913a89c64e4703ff709850d64feeed1c14e15");
        }
        else {
            throw new IllegalStateException("Unexpected value: " + quicVersion);
        }
    }

    public static byte[] createValidInitialNoPadding(Version quicVersion) {
        if (quicVersion.equals(IETF_draft_29)) {
            // ODCID: c3265ad62fae4bc3   Packet length: 283
            return ByteUtils.hexToBytes("cfff00001d08c3265ad62fae4bc308a2cd427a0a3e15f1004101e0423c8e5e598c0dcaef595d39197c99fd231ac8f64739fb1e9593cd80bf19fc315cccb6b98b065ce35a1c0c30cf6357f8518b69cd89e1d663fc82e0d63e0d1a7bf4d751d39222b35949eb4df26c9275c577ab3f0479e374afe4834a8168cf874d3ca96b17e4facffa65270285cd7257be0f2ecedaaebc2e88100574a1bd6b8a21e0055e584233187f9fc3f6545e69cff287e454b46b1cb41c53b1528ba511aa305ff2be7193248ac74d07e06da4cd5142af0d4db06753068079281767b7a76b3d3202c0c4120f9d338cb0e4a42fd95e42992765b72b45739f524bbd9d672b94f15a07a08de4c11dfb797e6f7fa592150dc0c005e55031791a33eaf66b91010bce");
        }
        else if (quicVersion.equals(QUIC_version_1)) {
            // Packet length: 312
            return ByteUtils.hexToBytes("c200000001084103b6cb8134334308c4ddcf9666dadb7300411e5be4fcc917d76496af17d4dd257170a37b9f544e76039dabb09a05d46febeceffc55dac5b79866b46670593f4d65605e8f7e2bfb7231599353d8af47f1f2f291331196b0e90b8c2d8d5d0a34e8007b2478404f1430b99fbbdf8ded030f9ae6e3b6892d5f10847fbbe91fce012df091bc77bc71a6d89e0e6f635853fcb28f1655322fe2ee02009092f34cbd6f631e251400914d142de2700b45702a443ac662ee7a3e6492f9cebfdf978382749e299b2d0b6bae4a08bf02935730abc843d0f2372e59440467e4ae761f021b76310521adb8114c1a5f4eed60445d89f470b0389f06ec12662c0bcfcdf9b897b7dc423be03ff068d01aa91108cc48c53f4f87ae2b8ab52d13a0c540639ab2abbc6f1703ad3460ec7b2dd272055bd035d403f3");
        }
        else {
            throw new IllegalStateException("Unexpected value: " + quicVersion);
        }
    }

    public static byte[] createInvalidInitial(Version quicVersion) {
        if (quicVersion.equals(IETF_draft_29)) {
            // ODCID: 67268378ae7dc13b   Packet length: 1200
            return ByteUtils.hexToBytes("cbff00001d0867268378ae7dc13b08d5aabb03c53eed7d0044966ba935ce00000000000000004d098f254ebecc3d2fc3c00b5f4a6f84ae435556be3b47443761a3ba8d454611fd4f8daa1206919c11a8c749cc1250c0d4642fa879f396fee00b9bb55faf81e712628348d6d99df13f3006a19bbc4a3100a4dc9ea0089bf9c9c50ede7d13c13d5c4cd0db4ba0b26c98ebb35beaf46950e7501db32db57f0f4b2c8e8905c8db9c884ad34e810221052ca4bad7385c11c6f99f6f7297b55b5ea032396c1207d97e66f2a11c6f4c0b8dcb6e7a337cb20a72893d3eadd571cf18a6dfc9985463ea26f02ab0d89b2401101bcfe334a00c13aafeb1ff5e78199ccec1e5e5fff747dff32227ae844abb6df9f6f383f8aa0fcfa6620212a53dc9693a9423ec1c8530773af6d403ebe650ed1c3496660be26acc4d7259f78db79e13c4aa8e886270d5da425fa8bbb4d6eecf6dbd95b7de021242b6194b6ad3659badc164284744c41e328681d632258bd8df0378f37ff1d8d98dc7523c185ed15bf1a18fe6e6e63e977a58c4a97ceb5ea4558f8f3242f0ef135300ca5e035c1576e164394ae8158114cd243db197e3471c4126006c80512d80052a1e5abefc89e4e67eeab86a684176ccc610c94b224c228c3cb38eba7890e4b983ed909352fba349dc0d994c4984a7c5acb0f34d6f872f18232f21f70f9d6328aa5a2db058b12b73a7fbee815235063db9960ab2ee65dd3924c6109d85eb825192a2ee16bb5c1fa14c0279d0b4d55a5414854a35a6003c94ff4e3a13b728df2a35b739e64ca070997db4319bcf414ae677e6a362177b3aae8bdebd652283b87ad2985cf0d20636561321c4e64e58eae9e723b02a880ce6d50ff180aca94c26fe092a5d94578c0283e90d7ad3901e25fa4e29fa88212eff3196e93bea9e8fc466a5cd58c8621fe303abffd669624d025787b674f38e80f07fb3cb6a27dcc2ee1ca2ba42944dcf3b9485e12bc3946bb3ac8a5a1765203e5949009cb04137b008dfc09f45f43b7bb21d96a0120600aa565ef074ae80b95e99dec947d948ce64066525906e5d2d9dceab57e4a76b4967c81399cabb28c3d119a7464e1e489c7e40a972ef4676155163924c85a1d52756f4b4018d218774e56e007d7e954675eb40ecad5745eeb8bc8c6dafa898dc6142d3997b5735418c2b9b81e1d70398ae0788d4a936775583b62ef777566f6e9ea71c6848e7aee12baf4c454fc3f57ffd2915aa0d9ea64d8d360ec86bcac5f8c8c5e793cad593d63d26283c32ea206ba3f4407865f0d0a6c7aa1d488b9ce0d32fd7d6469885422ccf228659b8ee566cda76520127b5395b6e7b911c8b0cdddc1d3e9dcaf08efdf1bf40c938b3f08d95d8974069575ddc3b81fef44de347b988cf6bdc33498192b70c2daf0c6ef43653ac96b7c95a2ffb3878985926ae19df2a6bfda342ce2622800fc1cce5a4b319bae73faa7455bd1a2b29df030b8bee6cac8d7a98cf20b6bb288604a0733a215a2bd395a781109cc0017bee36a2717f984ebaf09e9c788740a9fdab28cf66a7f84942c0ef49da17689697e7e6275577a2a22e9d0ba3e03bd56090f43a19493d6e997fc2678403174af27cbae35c113e6e74e583be8c34c03592fb61433ec573da50056ab707a86b293c9710394be5475d4bec3c087f8316b90151b894397d1475");
        }
        else if (quicVersion.equals(QUIC_version_1)) {
            return ByteUtils.hexToBytes("cb0000000108136c48a42e895aaa083020bd5f0c60edb6004496a262b8210000000000000000609db716705e38ce63b3fd80862117500b6ae639a6e7bf75bdc268cbfc778fc6ccb07f7849b0fa61d145d30faf5027d90ddf8a95a913b6b348af498301af73ad68d2ef493722e6c2064491ac1807b515deb65f1186dccc1a94da6d1301064c9eda47a0163fd388c28ce7897f4b0b03435122e322b981494e3e3389a67646a0e25cf0ed90bc149eb13e7fa229a568bbe3c630c88e88ea84c20160934eff117d6fd0c3aca03004e9d115e8026ae40a19c93ce5bf8c321656d43856a66bac5cd345bf7853a4bb2b3bc22b251f47c7b8609323c88d6dafa0d9d0f35518c66e6a8e1a77df0fa134c91ae0301dac90bfd2a96a2d6338310f74b7070209f41861e868bb2a82e2fde8b7825eebd8fb34b81cebd3208e034279fe32b8b6cec2c7f9a79403dbee1d4e4389f93f48e5b0c5139c0e1cd2a0754ae21c99c40a2c4e6c69b5f8c13e660ac6b5732d69e7b5593955c70f69a9e4c985c6b0f64d4af68e0d0a7291b89a1b846849f7a86346a57fcb0016a0d1f7a0fd176911222de38601d5a4e8bf2ae702418ee1aedb1aacca1cde034a1c23fd6b526ef767a6077ecc4e465987c294bcc28d386107eda1c0ace4519efec6ee72ab2d3215db090d3fd01052ca5adeded3c4ea900773b5afdaf8f7ba983e150375070e47ece30a9ec9402235d2866ae06bc467d741bace83ba42ea0bf8d946e71a66233886c1ca795a380f2ff1663b9598a1ead3c1d7e092aeed7b6ee1a9e1c4428ae53c0904d19e7bcca053bdfbd84e524ebec1f48d346af441b5cf70ecd309d2dd519fa7933420f2b3cc70828f168c2774f62382dc671b460ea5666a766fcd6ae98ede820e7afa21a987e3d2d5eec0832c28e0bf3b4bc2aa3eb05d3b8cfceac20bfdd4be5b1788e8005dfc9322fb897db1ec9faceb307c23952fbca862ca2fec925dfcda6c2e27066f027e140bf8fea1bdc7a4f6a86c78706dd8d7159af6c63291c548383db5940cb6a19047c31bbcdc765ebc5dcfbb2e2d899f43f33d41b5a931bf3ec74cf410f9919eb4a5af0ca2ccc25d294530b74a893ee97685949a3be9fbd10c222e0650a4307dce096db3679534874896c6b935313367b7579dd8771b08cf0ff702c549da89822ba6e516317f5ea67bcff55c76b301a8614dc8854d0f11ad262932142424c976dd41b4227e61a974d52a7f5c5e8ccf297463a6ebe32983a91ec365db34bb80365782f60fedfd2c5c77ea076203e7771d90cd8934cb64f5fda2d483f0a4b3c4f96bdf5cf75732f61dc96b46157ad37a2fb8ab05f7049d1d78780533b86094b46c58957c58a5a53dd2ad3188f3975de8739e967341429ffe59d3526f2b8f95b42fd09afdae5a3f15b6024504218dd1443aa44706c4d7e5b08c1f68cf610e3060301e789e79ca46e0f99fc6358d5ef635e7cd8683dfb024b3d87c7a632b069218cf5846c979b49d3f4f7a4b947c87b7e610d326cf7f1d0d36e3d0b52c1622645ca9971ce36bc5ea25d4185d5c478f923396b09a529e766f3d3e19c206ed6858b44341c18d61097d1a2086dc5f382cefe6a8a30fc3924f305dedfe2bef6713884d6acb73e569f398bcb39a5bb935f062c729fad7ee5e45356692647cf29cb913a89c64e4703ff709850d64feeed1c14e15");
        }
        else {
            throw new IllegalStateException("Unexpected value: " + quicVersion);
        }
    }
}

