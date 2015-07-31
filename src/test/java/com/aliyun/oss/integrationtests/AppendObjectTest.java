/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestConstants.MISSING_ARGUMENT_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.OBJECT_NOT_APPENDABLE_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.POSITION_NOT_EQUAL_TO_LENGTH_ERROR;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;

import java.io.File;
import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.OSSObject;

public class AppendObjectTest extends TestBase {
	
	private static final String APPENDABLE_OBJECT_TYPE = "Appendable";
	
	@Test
	public void testNormalAppendObject() throws Exception {		
		String key = "normal-append-object";
		final long instreamLength = 128 * 1024;
		
		try {
			// Usage style  1
			InputStream instream = genFixedLengthInputStream(instreamLength);
			AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
			appendObjectRequest.setPosition(0L);
			AppendObjectResult appendObjectResult = secondClient.appendObject(appendObjectRequest);
			OSSObject o = secondClient.getObject(bucketName, key);
			Assert.assertEquals(key, o.getKey());
			Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
			Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
			if (appendObjectResult.getNextPosition() != null) {
				Assert.assertEquals(instreamLength, appendObjectResult.getNextPosition().longValue());
			}
			
			// Usage style 2
			final String filePath = genFixedLengthFile(instreamLength);
			appendObjectRequest = new AppendObjectRequest(bucketName, key, new File(filePath));
			appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
			appendObjectResult = secondClient.appendObject(appendObjectRequest);
			o = secondClient.getObject(bucketName, key);
			Assert.assertEquals(instreamLength * 2, o.getObjectMetadata().getContentLength());
			Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
			if (appendObjectResult.getNextPosition() != null) {				
				Assert.assertEquals(instreamLength * 2, appendObjectResult.getNextPosition().longValue());
			}
		} catch (Exception ex) {
			Assert.fail(ex.getMessage());
		}
	}
	
	@Test
	public void testAppendExistingNormalObject() throws Exception {		
		String key = "append-existing-normal-object";
		final long instreamLength = 128 * 1024;
		
		try {
			InputStream instream = genFixedLengthInputStream(instreamLength);
			secondClient.putObject(bucketName, key, instream, null);
			OSSObject o = secondClient.getObject(bucketName, key);
			Assert.assertEquals(key, o.getKey());
			Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
			
			try {
				instream = genFixedLengthInputStream(instreamLength);
				AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
				appendObjectRequest.setPosition(instreamLength);
				secondClient.appendObject(appendObjectRequest);
			} catch (OSSException ex) {
				Assert.assertEquals(OSSErrorCode.OBJECT_NOT_APPENDALBE, ex.getErrorCode());
				Assert.assertTrue(ex.getMessage().startsWith(OBJECT_NOT_APPENDABLE_ERR));
			}
		} catch (Exception ex) {
			Assert.fail(ex.getMessage());
		}
	}
	
	@Test
	public void testAppendObjectAtIllegalPosition() throws Exception {		
		String key = "append-object-at-illlegal-position";
		final long instreamLength = 128 * 1024;
		
		try {
			InputStream instream = genFixedLengthInputStream(instreamLength);
			AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
			appendObjectRequest.setPosition(0L);
			AppendObjectResult appendObjectResult = secondClient.appendObject(appendObjectRequest);
			OSSObject o = secondClient.getObject(bucketName, key);
			Assert.assertEquals(key, o.getKey());
			Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
			Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
			if (appendObjectResult.getNextPosition() != null) {
				Assert.assertEquals(instreamLength, appendObjectResult.getNextPosition().longValue());
			}
			
			try {
				instream = genFixedLengthInputStream(instreamLength);
				appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
				// Set illegal postion to append, here should be 'instreamLength' rather than other positions.
				appendObjectRequest.setPosition(instreamLength - 1);		
				secondClient.appendObject(appendObjectRequest);
			} catch (OSSException ex) {
				Assert.assertEquals(OSSErrorCode.POSITION_NOT_EQUAL_TO_LENGTH, ex.getErrorCode());
				Assert.assertTrue(ex.getMessage().startsWith(POSITION_NOT_EQUAL_TO_LENGTH_ERROR));
			}
		} catch (Exception ex) {
			Assert.fail(ex.getMessage());
		}
	}
	
	@Ignore
	public void testAppendObjectMissingArguments() throws Exception {		
		String key = "append-object-missing-arguments";
		final long instreamLength = 128 * 1024;
		
		try {
			Assert.assertEquals(true, secondClient.doesBucketExist(bucketName));
			InputStream instream = genFixedLengthInputStream(instreamLength);
			AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
			// Missing required parameter 'postition'
			secondClient.appendObject(appendObjectRequest);
		} catch (OSSException ex) {
			Assert.assertEquals(OSSErrorCode.MISSING_ARGUMENT, ex.getErrorCode());
			Assert.assertTrue(ex.getMessage().startsWith(MISSING_ARGUMENT_ERR));
		}
	}
}
