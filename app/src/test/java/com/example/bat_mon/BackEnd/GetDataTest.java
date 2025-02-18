package com.example.bat_mon.BackEnd;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.Exceptions.GetDataException;
import com.example.bat_mon.UnitTestData;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class GetDataTest {

    private static MockWebServer server;
    private static HttpUrl baseUrl;

    /**
     * This will check if the Base64 data stream is correctly converted into hex-formatted data frames
     * We will have a predefined input and check the results against a predefined dataFrames array
     * @throws InvocationTargetException when the convertBase64ToHex method cannot be invoked
     * @throws IllegalAccessException when the convertBase64ToHex method cannot be invoked
     * @throws NoSuchMethodException when invoking the convertBase64ToHex function fails
     */
    @Test
    public void convertBase64ToHex_isCorrect() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method convertBase64ToHex = GetData.class.getDeclaredMethod("convertBase64ToHex", byte[].class);
        convertBase64ToHex.setAccessible(true);
        byte[] decoded = Base64.getDecoder().decode(UnitTestData.base64);
        String[] dataFrames = (String[]) convertBase64ToHex.invoke(null, decoded);
        Assert.assertArrayEquals(dataFrames, UnitTestData.dataFramesExpected);
    }

    /**
     * @throws IllegalAccessException when invoking the checksum function fails
     * @throws NoSuchMethodException when invoking the checksum function fails
     * @throws InvocationTargetException when the checksum function throws an unexpected exception
     * Checks if the checksum is calculated correctly. We have a predefined dataStream and a predefined checksum.
     * When the calculated and predefined checksums match, the test passes.
     */
    @Test(expected = Test.None.class)
    public void checksum_isCorrect() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method checksum = GetData.class.getDeclaredMethod("checksum", byte[].class, String[].class);
        checksum.setAccessible(true);
        byte[] decoded = Base64.getDecoder().decode(UnitTestData.base64);
        checksum.invoke(null, decoded, UnitTestData.dataFramesExpected);
    }

    /**
     * @throws GetDataException when checksums don't match, which is expected
     * @throws IllegalAccessException when invoking the checksum function fails
     * @throws NoSuchMethodException when invoking the checksum function fails
     * Checks if the checksum method throws an exception when the data steam is incorrect.
     */
    @Test(expected = GetDataException.class)
    public void checksum_notCorrect() throws GetDataException, IllegalAccessException, NoSuchMethodException {
        try {
            Method checksum = GetData.class.getDeclaredMethod("checksum", byte[].class, String[].class);
            checksum.setAccessible(true);
            byte[] decoded = Base64.getDecoder().decode(UnitTestData.base64);
            decoded[100] = 100;
            checksum.invoke(null, decoded, UnitTestData.dataFramesExpected);
        } catch (InvocationTargetException e) {
            Assert.assertTrue(e.getCause().getMessage().contains("Checksums did not match"));
            throw new GetDataException("Checksums did not match", ErrorCode.CHECKSUMS_DONT_MATCH);
        }
    }

    @BeforeClass
    public static void setUpMockServer() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("/live");
    }

    @AfterClass
    public static void shutDownMockServer() throws IOException {
        server.shutdown();
    }

    /**
     * Check if the GetData class sends the correct get request and receives the correct data
     * @throws InterruptedException when we cannot read the request from the mock server
     * @throws GetDataException when the get request failed
     */
    @Test
    public void sendGet_isCorrect() throws InterruptedException, GetDataException {
        // Server will answer with base64 data stream
        server.enqueue(new MockResponse().setBody(UnitTestData.base64));

        // Make request to server and get data
        String[] dataFrames = GetData.getData(baseUrl.toString(), "testuser", "testpassword");
        Assert.assertArrayEquals(UnitTestData.dataFramesExpected, dataFrames);

        // Make sure the first request to the server was the authorization
        RecordedRequest request = server.takeRequest();
        Assert.assertEquals("/live", request.getPath());
        Assert.assertNotNull(request.getHeader("Authorization"));
    }

    /**
     * Check if the correct exception is thrown, when authorization with the server fails
     * @throws GetDataException When the get request fails, which is expected
     */
    @Test(expected = GetDataException.class)
    public void sendGet_authorizationFailed() throws GetDataException {
        // Server will answer with response code 403, which means "Not authorized"
        server.enqueue(new MockResponse().setResponseCode(403));

        // Make request to server
        GetData.getData(baseUrl.toString(), "testuser", "testpassword");
    }

}
