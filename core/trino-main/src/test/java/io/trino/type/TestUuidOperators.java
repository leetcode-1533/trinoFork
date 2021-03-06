/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.type;

import io.trino.operator.scalar.AbstractTestFunctions;
import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import org.testng.annotations.Test;

import static io.airlift.slice.Slices.utf8Slice;
import static io.trino.spi.function.OperatorType.HASH_CODE;
import static io.trino.spi.function.OperatorType.INDETERMINATE;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.UuidType.UUID;
import static io.trino.spi.type.VarbinaryType.VARBINARY;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static io.trino.testing.SqlVarbinaryTestingUtil.sqlVarbinaryFromHex;
import static io.trino.type.UuidOperators.castFromVarcharToUuid;
import static org.testng.Assert.assertEquals;

public class TestUuidOperators
        extends AbstractTestFunctions
{
    @Test
    public void testRandomUuid()
    {
        tryEvaluateWithAll("uuid()", UUID);
    }

    @Test
    public void testVarcharToUUIDCast()
    {
        assertFunction("CAST('00000000-0000-0000-0000-000000000000' AS UUID)", UUID, "00000000-0000-0000-0000-000000000000");
        assertFunction("CAST('12151fd2-7586-11e9-8f9e-2a86e4085a59' AS UUID)", UUID, "12151fd2-7586-11e9-8f9e-2a86e4085a59");
        assertFunction("CAST('300433ad-b0a1-3b53-a977-91cab582458e' AS UUID)", UUID, "300433ad-b0a1-3b53-a977-91cab582458e");
        assertFunction("CAST('d3074e99-de12-4b8c-a2a1-b7faf79faba6' AS UUID)", UUID, "d3074e99-de12-4b8c-a2a1-b7faf79faba6");
        assertFunction("CAST('dfa7eaf8-6a26-5749-8d36-336025df74e8' AS UUID)", UUID, "dfa7eaf8-6a26-5749-8d36-336025df74e8");
        assertFunction("CAST('12151FD2-7586-11E9-8F9E-2A86E4085A59' AS UUID)", UUID, "12151fd2-7586-11e9-8f9e-2a86e4085a59");
        assertInvalidCast("CAST('1-2-3-4-1' AS UUID)", "Invalid UUID string length: 9");
        assertInvalidCast("CAST('12151fd217586211e938f9e42a86e4085a59' AS UUID)", "Cannot cast value to UUID: 12151fd217586211e938f9e42a86e4085a59");
    }

    @Test
    public void testUUIDToVarcharCast()
    {
        assertFunction("CAST(UUID 'd3074e99-de12-4b8c-a2a1-b7faf79faba6' AS VARCHAR)", VARCHAR, "d3074e99-de12-4b8c-a2a1-b7faf79faba6");
        assertFunction("CAST(CAST('d3074e99-de12-4b8c-a2a1-b7faf79faba6' AS UUID) AS VARCHAR)", VARCHAR, "d3074e99-de12-4b8c-a2a1-b7faf79faba6");
    }

    @Test
    public void testVarbinaryToUUIDCast()
    {
        assertFunction("CAST(x'00000000000000000000000000000000' AS UUID)", UUID, "00000000-0000-0000-0000-000000000000");
        assertFunction("CAST(x'12151fd2758611e98f9e2a86e4085a59' AS UUID)", UUID, "12151fd2-7586-11e9-8f9e-2a86e4085a59");
        assertFunction("CAST(x'300433adb0a13b53a97791cab582458e' AS UUID)", UUID, "300433ad-b0a1-3b53-a977-91cab582458e");
        assertFunction("CAST(x'd3074e99de124b8ca2a1b7faf79faba6' AS UUID)", UUID, "d3074e99-de12-4b8c-a2a1-b7faf79faba6");
        assertFunction("CAST(x'dfa7eaf86a2657498d36336025df74e8' AS UUID)", UUID, "dfa7eaf8-6a26-5749-8d36-336025df74e8");
        assertFunction("CAST(x'12151fd2758611e98f9e2a86e4085a59' AS UUID)", UUID, "12151fd2-7586-11e9-8f9e-2a86e4085a59");
        assertInvalidCast("CAST(x'f000001100' AS UUID)", "Invalid UUID binary length: 5");
    }

    @Test
    public void testUUIDToVarbinaryCast()
    {
        assertFunction("CAST(UUID '00000000-0000-0000-0000-000000000000' AS VARBINARY)", VARBINARY, sqlVarbinaryFromHex("00000000000000000000000000000000"));
        assertFunction("CAST(UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS VARBINARY)", VARBINARY, sqlVarbinaryFromHex("6B5F5B6567E443B08EE3586CD49F58A0"));
    }

    @Test
    public void testEquals()
    {
        assertFunction("UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' = UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0'", BOOLEAN, true);
        assertFunction("CAST('6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS UUID) = CAST('6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS UUID)", BOOLEAN, true);
    }

    @Test
    public void testDistinctFrom()
    {
        assertFunction("UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' IS DISTINCT FROM UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0'", BOOLEAN, false);
        assertFunction("CAST(NULL AS UUID) IS DISTINCT FROM CAST(NULL AS UUID)", BOOLEAN, false);
        assertFunction("UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' IS DISTINCT FROM UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a1'", BOOLEAN, true);
        assertFunction("UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' IS DISTINCT FROM CAST(NULL AS UUID)", BOOLEAN, true);
        assertFunction("CAST(NULL AS UUID) IS DISTINCT FROM UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0'", BOOLEAN, true);
    }

    @Test
    public void testNotEquals()
    {
        assertFunction("UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' != UUID '12151fd2-7586-11e9-8f9e-2a86e4085a59'", BOOLEAN, true);
        assertFunction("CAST('12151fd2-7586-11e9-8f9e-2a86e4085a59' AS UUID) != UUID '12151fd2-7586-11e9-8f9e-2a86e4085a59'", BOOLEAN, false);
    }

    @Test
    public void testOrderOperators()
    {
        assertFunction("CAST('12151fd2-7586-11e9-8f9e-2a86e4085a58' AS UUID) < CAST('12151fd2-7586-11e9-8f9e-2a86e4085a59' AS UUID)", BOOLEAN, true);
        assertFunction("CAST('12151fd2-7586-11e9-8f9e-2a86e4085a59' AS UUID) < CAST('12151fd2-7586-11e9-8f9e-2a86e4085a58' AS UUID)", BOOLEAN, false);

        assertFunction("CAST('12151fd2-7586-11e9-8f9e-2a86e4085a58' AS UUID) < CAST('6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS UUID)", BOOLEAN, true);
        assertFunction("CAST('6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS UUID) < CAST('12151fd2-7586-11e9-8f9e-2a86e4085a58' AS UUID)", BOOLEAN, false);

        assertFunction("CAST('dfa7eaf8-6a26-5749-8d36-336025df74e8' AS UUID) > CAST('6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS UUID)", BOOLEAN, true);
        assertFunction("CAST('6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AS UUID) > CAST('dfa7eaf8-6a26-5749-8d36-336025df74e8' AS UUID)", BOOLEAN, false);

        assertFunction("UUID '12151fd2-7586-11e9-8f9e-2a86e4085a52' BETWEEN UUID '12151fd2-7586-11e9-8f9e-2a86e4085a50' AND UUID '12151fd2-7586-11e9-8f9e-2a86e4085a59'", BOOLEAN, true);
        assertFunction("UUID '12151fd2-7586-11e9-8f9e-2a86e4085a52' BETWEEN UUID '12151fd2-7586-11e9-8f9e-2a86e4085a54' AND UUID '12151fd2-7586-11e9-8f9e-2a86e4085a59'", BOOLEAN, false);

        assertFunction("UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' BETWEEN UUID '12151fd2-7586-11e9-8f9e-2a86e4085a50' AND UUID 'dfa7eaf8-6a26-5749-8d36-336025df74e8'", BOOLEAN, true);
        assertFunction("UUID '12151fd2-7586-11e9-8f9e-2a86e4085a52' BETWEEN UUID '6b5f5b65-67e4-43b0-8ee3-586cd49f58a0' AND UUID 'dfa7eaf8-6a26-5749-8d36-336025df74e8'", BOOLEAN, false);
    }

    @Test
    public void testCompare()
    {
        assertEquals(uuidCompare("12151fd2-7586-11e9-8f9e-2a86e4085a58", "12151fd2-7586-11e9-8f9e-2a86e4085a59"), -1);
        assertEquals(uuidCompare("12151fd2-7586-11e9-8f9e-2a86e4085a58", "12151fd2-7586-11e9-8f9e-2a86e4085a58"), 0);
        assertEquals(uuidCompare("dfa7eaf8-6a26-5749-8d36-336025df74e8", "6b5f5b65-67e4-43b0-8ee3-586cd49f58a0"), 1);
        assertEquals(uuidCompare("12151fd2-7586-11e9-8f9e-2a86e4085a58", "dfa7eaf8-6a26-5749-8d36-336025df74e8"), -1);
    }

    private int uuidCompare(String uuidLeft, String uuidRight)
    {
        return (int) functionAssertions.getBlockTypeOperators()
                .getComparisonUnorderedLastOperator(UUID)
                .compare(uuidBlock(uuidLeft), 0, uuidBlock(uuidRight), 0);
    }

    @Test
    public void testIndeterminate()
    {
        assertOperator(INDETERMINATE, "CAST(null AS UUID)", BOOLEAN, true);
        assertOperator(INDETERMINATE, "UUID '12151fd2-7586-11e9-8f9e-2a86e4085a59'", BOOLEAN, false);
    }

    @Test
    public void testHash()
    {
        assertOperator(HASH_CODE, "CAST(null AS UUID)", BIGINT, null);
        assertOperator(HASH_CODE, "UUID '12151fd2-7586-11e9-8f9e-2a86e4085a59'", BIGINT, hashFromType("12151fd2-7586-11e9-8f9e-2a86e4085a59"));
    }

    private long hashFromType(String uuidString)
    {
        return functionAssertions.getBlockTypeOperators()
                .getHashCodeOperator(UUID)
                .hashCode(uuidBlock(uuidString), 0);
    }

    private static Block uuidBlock(String uuidString)
    {
        BlockBuilder blockBuilder = UUID.createBlockBuilder(null, 1);
        UUID.writeSlice(blockBuilder, castFromVarcharToUuid(utf8Slice(uuidString)));
        return blockBuilder.build();
    }
}
