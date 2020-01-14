package org.hisp.dhis.audit;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.Sets;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.util.Timer;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AuditRepositoryTest extends IntegrationTestBase
{
    @Autowired
    private AuditRepository auditRepository;

    @Test
    public void testSaveAudit()
    {
        String uid = CodeGenerator.generateUid();
        String code = CodeGenerator.generateUid();

        Audit audit = Audit.builder()
            .auditType( AuditType.CREATE )
            .auditScope( AuditScope.AGGREGATE )
            .createdAt( LocalDateTime.of( 2019, 1, 1, 0, 0 ) )
            .createdBy( "test-user" )
            .klass( DataElement.class.getName() )
            .uid( uid )
            .code( code )
            .data( "{}" )
            .build();

        auditRepository.save( audit );
        assertEquals( 1, auditRepository.query( AuditQuery.builder().build() ).size() );
    }

    @Test
    public void testDeleteAudit()
    {
        String uid = CodeGenerator.generateUid();
        String code = CodeGenerator.generateUid();

        Audit audit = Audit.builder()
            .auditType( AuditType.CREATE )
            .auditScope( AuditScope.AGGREGATE )
            .createdAt( LocalDateTime.of( 2019, 1, 1, 0, 0 ) )
            .createdBy( "test-user" )
            .klass( DataElement.class.getName() )
            .uid( uid )
            .code( code )
            .data( "{}" )
            .build();

        long id = auditRepository.save( audit );
        assertEquals( 1, id );

        audit.setId( id );
        auditRepository.delete( audit );

        List<Audit> audits = auditRepository.query( AuditQuery.builder().build() );
        assertTrue( audits.isEmpty() );
    }

    @Test
    public void testAuditQueryAll()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder().build() );

        assertEquals( 100, audits.size() );
    }

    @Test
    public void testAuditQueryAuditType()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .auditType( Sets.newHashSet( AuditType.CREATE ) )
            .build() );

        assertEquals( 100, audits.size() );
    }

    @Test
    public void testAuditQueryAuditTypeNoMatch()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .auditType( Sets.newHashSet( AuditType.UPDATE ) )
            .build() );

        assertTrue( audits.isEmpty() );
    }

    @Test
    public void testAuditQueryCountAuditType()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .auditType( Sets.newHashSet( AuditType.CREATE ) )
            .build() );

        assertEquals( 100, audits );
    }

    @Test
    public void testAuditQueryCountAuditTypeNoMatch()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .auditType( Sets.newHashSet( AuditType.UPDATE ) )
            .build() );

        assertEquals( 0, audits );
    }

    @Test
    public void testAuditQueryAuditScope()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .auditScope( Sets.newHashSet( AuditScope.AGGREGATE ) )
            .build() );

        assertEquals( 100, audits.size() );
    }

    @Test
    public void testAuditQueryAuditScopeNoMatch()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .auditScope( Sets.newHashSet( AuditScope.TRACKER ) )
            .build() );

        assertTrue( audits.isEmpty() );
    }

    @Test
    public void testAuditQueryCountAuditScope()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .auditScope( Sets.newHashSet( AuditScope.AGGREGATE ) )
            .build() );

        assertEquals( 100, audits );
    }

    @Test
    public void testAuditQueryKlasses()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .klass( Sets.newHashSet( DataElement.class.getName(), OrganisationUnit.class.getName() ) )
            .build() );

        assertEquals( 100, audits.size() );
    }

    @Test
    public void testAuditQueryUids()
    {
        List<String> uids = new ArrayList<>();

        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            uids.add( uid );

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .uid( new HashSet<>( uids.subList( 0, 50 ) ) )
            .build() );

        assertEquals( 50, audits.size() );
    }

    @Test
    public void testAuditQueryCodes()
    {
        List<String> codes = new ArrayList<>();

        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            codes.add( code );

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .code( new HashSet<>( codes.subList( 50, 100 ) ) )
            .build() );

        assertEquals( 50, audits.size() );
    }

    @Test
    public void testAuditQueryCountKlasses()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .klass( Sets.newHashSet( DataElement.class.getName(), OrganisationUnit.class.getName() ) )
            .build() );

        assertEquals( 100, audits );
    }

    @Test
    public void testAuditQueryCountUids()
    {
        List<String> uids = new ArrayList<>();

        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            uids.add( uid );

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .uid( new HashSet<>( uids.subList( 50, 100 ) ) )
            .build() );

        assertEquals( 50, audits );
    }

    @Test
    public void testAuditQueryCountCodes()
    {
        List<String> codes = new ArrayList<>();

        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            codes.add( code );

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .code( new HashSet<>( codes.subList( 50, 100 ) ) )
            .build() );

        assertEquals( 50, audits );
    }

    @Test
    public void testAuditQueryDateRange()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        int audits = auditRepository.count( AuditQuery.builder()
            .range( AuditQuery.range( LocalDateTime.of( 2050, 1, 1, 0, 0, 0 ) ) )
            .build() );

        assertEquals( 50, audits );

        audits = auditRepository.count( AuditQuery.builder()
            .range( AuditQuery.range(
                LocalDateTime.of( 2050, 1, 1, 0, 0, 0 ),
                LocalDateTime.of( 2080, 1, 1, 0, 0, 0 )
            ) )
            .build() );

        assertEquals( 30, audits );
    }

    @Test
    public void testCompressDecompress()
    {
        String uid = CodeGenerator.generateUid();
        String code = CodeGenerator.generateUid();

        AuditAttributes attributes = new AuditAttributes();
        attributes.put( "path", "a.b.c" );

        Audit audit = Audit.builder()
            .auditType( AuditType.CREATE )
            .auditScope( AuditScope.AGGREGATE )
            .createdAt( LocalDateTime.of( 2019, 1, 1, 0, 0 ) )
            .createdBy( "test-user" )
            .klass( DataElement.class.getName() )
            .uid( uid )
            .code( code )
            .attributes( attributes )
            .data( "This is a message" )
            .build();

        auditRepository.save( audit );
        List<Audit> query = auditRepository.query( AuditQuery.builder().build() );
        assertEquals( 1, query.size() );

        Audit persistedAudit = query.get( 0 );

        assertEquals( uid, persistedAudit.getUid() );
        assertEquals( code, persistedAudit.getCode() );
        assertEquals( "This is a message", persistedAudit.getData() );

        assertNotNull( persistedAudit.getAttributes() );
        assertEquals( "a.b.c", persistedAudit.getAttributes().get( "path" ) );
    }

    @Test
    @Ignore
    public void testAuditInsert200k()
    {
        List<Audit> audits = new ArrayList<>();

        IntStream.rangeClosed( 1, 200_000 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 2000, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            audits.add( audit );
        } );

        Timer timer = new Timer().start();
        audits.forEach( audit -> auditRepository.save( audit ) );
        System.err.println( "Single Insert: " + timer.getTimeInS() + "s" );

        assertEquals( 200_000, auditRepository.count( AuditQuery.builder().build() ) );
    }

    @Test
    @Ignore
    public void testAuditInsertBatch200k()
    {
        List<Audit> audits = new ArrayList<>();

        IntStream.rangeClosed( 1, 200_000 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 2000, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            audits.add( audit );
        } );

        Timer timer = new Timer().start();
        auditRepository.save( audits );
        System.err.println( "Batch Insert: " + timer.getTimeInS() + "s" );

        assertEquals( 200_000, auditRepository.count( AuditQuery.builder().build() ) );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}
