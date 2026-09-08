package com.pvtalent.esign.service;

import com.pvtalent.esign.model.Agreement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgreementPdfGenerator {
    private static final float PAGE_W=PDRectangle.A4.getWidth(), PAGE_H=PDRectangle.A4.getHeight();
    private static final float LEFT=55.1f, BODY_LEFT=73.1f, RIGHT=540f, BODY_SIZE=10.5f, LEADING=15f;

    public String generate(Agreement a,String uploadDir)throws Exception{
        Path dir=Paths.get(uploadDir).toAbsolutePath().normalize();Files.createDirectories(dir);Path output=dir.resolve(UUID.randomUUID()+"-agreement.pdf");
        try(PDDocument doc=new PDDocument()){pageOne(doc,a);pageTwo(doc);pageThree(doc,a);doc.save(output.toFile());}return output.toString();
    }
    private void pageOne(PDDocument doc,Agreement a)throws Exception{
        PDPage page=new PDPage(PDRectangle.A4);doc.addPage(page);try(PDPageContentStream cs=new PDPageContentStream(doc,page)){
            centered(cs,"PV TALENT PARTNERS",55,16,PDType1Font.HELVETICA_BOLD);centered(cs,"AI-Powered Recruitment Consultancy",75,10,PDType1Font.HELVETICA);centered(cs,"Ref: "+safe(a.getAgreementNumber()),88,10,PDType1Font.HELVETICA);centered(cs,"CANDIDATE PLACEMENT SERVICES AGREEMENT",126,12,PDType1Font.HELVETICA_BOLD);
            float y=170;
            y=paragraph(cs,"This Candidate Placement Services Agreement (\"Agreement\") is entered into on "+value(a.getExecutionDate())+" (\"Execution Date\") by and between:",LEFT,y,RIGHT,BODY_SIZE);y-=10;
            y=paragraph(cs,"PV TALENT PARTNERS, represented by Mr. BaluRaju P V, Proprietor, hereinafter referred to as the \"Consultant\";",LEFT,y,RIGHT,BODY_SIZE);y-=10;text(cs,"AND",LEFT,y,BODY_SIZE,PDType1Font.HELVETICA);y-=22;
            y=paragraph(cs,value(a.getCandidateFullName())+", residing at "+value(a.getCandidateAddress())+", holding "+idText(a)+", hereinafter referred to as the \"Candidate\".",LEFT,y,RIGHT,BODY_SIZE);y-=10;
            y=paragraph(cs,"The Consultant and the Candidate are hereinafter individually referred to as a \"Party\" and collectively as the \"Parties\".",LEFT,y,RIGHT,BODY_SIZE);
            y=heading(cs,"1. PURPOSE",y-18);
            y=paragraph(cs,"1.1  The Consultant agrees to provide the Candidate with recruitment support services, including resume preparation, profile marketing, interview coordination, and placement assistance, with a view to securing suitable employment opportunities for the Candidate (\"Services\").",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=paragraph(cs,"1.2  This Agreement records the fee structure, payment terms, and refund conditions applicable to the Services.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=heading(cs,"2. FEES AND PAYMENT TERMS",y-18);
            y=paragraph(cs,"2.1  The total consultancy fee payable by the Candidate for the Services is "+value(a.getTotalFeeAmount())+" Rs. (\"Total Fee\").",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=paragraph(cs,"2.2  The Candidate shall pay "+value(a.getPrePaymentAmount())+" (\"Pre-Payment Amount\") to the Consultant Rs. on or before the Execution Date, prior to commencement of Services.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=paragraph(cs,"2.3  The remaining balance of the Total Fee, i.e., "+value(a.getBalanceAmount())+" (\"Balance Amount\"), shall Rs. become due and payable strictly in accordance with Clause 5 below.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=paragraph(cs,"2.4  \"Working Days\" for the purposes of this Agreement excludes Sundays and public holidays, and shall be counted from the date of receipt of the Pre-Payment Amount by the Consultant.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=heading(cs,"3. FORFEITURE ON MULTIPLE INTERVIEWS",y-18);
            y=paragraph(cs,"3.1  If the Candidate attends more than three (3) interviews arranged or facilitated by the Consultant within sixty (60) Working Days from the date of receipt of the Pre-Payment Amount, thirty percent (30%) of the Pre-Payment Amount shall stand forfeited and shall not be refundable to the Candidate, irrespective of the outcome of such interviews.",BODY_LEFT,y,RIGHT,BODY_SIZE);
            paragraph(cs,"3.2  The forfeiture under Clause 3.1 shall equally apply if the Candidate voluntarily withdraws from, discontinues, or drops out of the recruitment process at any stage after having attended more than three (3) such interviews.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
        }
    }
    private void pageTwo(PDDocument doc)throws Exception{
        PDPage page=new PDPage(PDRectangle.A4);doc.addPage(page);try(PDPageContentStream cs=new PDPageContentStream(doc,page)){
            float y=52;y=paragraph(cs,"3.3  For clarity, the remaining seventy percent (70%) of the Pre-Payment Amount shall continue to be governed by Clauses 4 and 5 below, as applicable.",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=heading(cs,"4. REFUND WHERE NO INTERVIEW IS SCHEDULED",y-18);y=paragraph(cs,"4.1  If no interview is scheduled or conducted for the Candidate within sixty (60) Working Days from the date of receipt of the Pre-Payment Amount, the Consultant shall refund the full Pre-Payment Amount to the Candidate.",BODY_LEFT,y,RIGHT,BODY_SIZE);y=paragraph(cs,"4.2  Such refund shall be made within a further period of twenty (20) Working Days immediately following the expiry of the 60 Working Day period referred to in Clause 4.1 (i.e., on or before the 80th Working Day from the date of receipt of the Pre-Payment Amount).",BODY_LEFT,y-8,RIGHT,BODY_SIZE);y=paragraph(cs,"4.3  No deduction shall be made from the refund under this Clause 4, except any statutory deduction required by law.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=heading(cs,"5. SETTLEMENT ON OFFER / DATE OF JOINING",y-18);y=paragraph(cs,"5.1  Upon the Candidate receiving a valid offer letter from an employer introduced by the Consultant, or upon the Candidate’s Date of Joining (DOJ) with such employer, whichever is earlier, the Balance Amount referred to in Clause 2.3 shall become immediately due and payable by the Candidate to the Consultant.",BODY_LEFT,y,RIGHT,BODY_SIZE);y=paragraph(cs,"5.2  In such event, the Pre-Payment Amount already paid by the Candidate shall stand adjusted against the Total Fee and shall not be refunded to the Candidate under any circumstances.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);y=paragraph(cs,"5.3  If the Candidate fails to pay the Balance Amount within seven (7) Working Days of receiving the offer letter or the DOJ, whichever is earlier, the Consultant reserves the right to pursue recovery of the Balance Amount through appropriate legal means, and the Pre-Payment Amount shall in any event remain non-refundable.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=heading(cs,"6. GENERAL FORFEITURE PRINCIPLE",y-18);y=paragraph(cs,"6.1  Save and except as expressly provided in Clause 4 (no-interview refund) and Clause 3.3 (partial forfeiture), the Pre-Payment Amount shall not be refundable to the Candidate once the Candidate has engaged with the recruitment process facilitated by the Consultant.",BODY_LEFT,y,RIGHT,BODY_SIZE);y=paragraph(cs,"6.2  No refund shall be due where the Candidate rejects a suitable offer secured through the Consultant’s efforts, or fails to attend a scheduled interview without reasonable prior notice to the Consultant.",BODY_LEFT,y-8,RIGHT,BODY_SIZE);
            y=heading(cs,"7. OBLIGATIONS OF THE CANDIDATE",y-18);y=bullet(cs,"Provide accurate and complete information regarding qualifications, experience, and skills.",y);y=bullet(cs,"Attend scheduled interviews punctually and inform the Consultant promptly of any inability to attend.",y);y=bullet(cs,"Not engage any other recruitment consultant for the same employer opportunities introduced by the Consultant, during the term of this Agreement.",y);y=bullet(cs,"Not misrepresent facts to prospective employers introduced by the Consultant.",y);
            y=heading(cs,"8. OBLIGATIONS OF THE CONSULTANT",y-18);bullet(cs,"Use reasonable efforts and professional diligence to identify suitable opportunities matching the Candidate’s profile.",y);bullet(cs,"Coordinate interview scheduling and communication between the Candidate and prospective employers.",y);
        }
    }
    private void pageThree(PDDocument doc,Agreement a)throws Exception{
        PDPage page=new PDPage(PDRectangle.A4);doc.addPage(page);try(PDPageContentStream cs=new PDPageContentStream(doc,page)){
            float y=52;y=bullet(cs,"Maintain confidentiality of the Candidate’s personal and professional information, save where disclosure is necessary for placement purposes or required by law.",y);y=bullet(cs,"Provide the Candidate with fair and accurate information regarding prospective employers and roles.",y);
            y=heading(cs,"9. CONFIDENTIALITY",y-18);y=paragraph(cs,"9.1  Both Parties shall keep confidential all information exchanged in the course of this engagement and shall not disclose the same to any third party, except as necessary for the performance of this Agreement or as required by law.",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=heading(cs,"10. RELATIONSHIP OF PARTIES",y-18);y=paragraph(cs,"10.1  This Agreement does not create any employer-employee, agency, partnership, or fiduciary relationship between the Consultant and the Candidate. The Consultant acts solely as a recruitment facilitator.",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=heading(cs,"11. TERMINATION",y-18);y=paragraph(cs,"11.1  Either Party may terminate this Agreement by giving seven (7) days’ prior written notice to the other Party, without prejudice to any refund or forfeiture obligations that have already accrued under Clauses 3, 4, and 5 as of the date of such notice.",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=heading(cs,"12. GOVERNING LAW AND JURISDICTION",y-18);y=paragraph(cs,"12.1  This Agreement shall be governed by the laws of India. Any dispute arising out of or in connection with this Agreement shall be subject to the exclusive jurisdiction of the courts at Bengaluru, Karnataka.",BODY_LEFT,y,RIGHT,BODY_SIZE);
            y=heading(cs,"13. ENTIRE AGREEMENT",y-18);y=paragraph(cs,"13.1  This Agreement constitutes the entire understanding between the Parties regarding its subject matter and supersedes all prior discussions. No amendment shall be valid unless made in writing and signed by both Parties.",BODY_LEFT,y,RIGHT,BODY_SIZE);paragraph(cs,"IN WITNESS WHEREOF, the Parties have executed this Agreement on the Execution Date first written above.",LEFT,y-18,RIGHT,BODY_SIZE);
            text(cs,"For PV TALENT PARTNERS",60.5f,535,BODY_SIZE,PDType1Font.HELVETICA_BOLD);text(cs,"CANDIDATE",303f,535,BODY_SIZE,PDType1Font.HELVETICA_BOLD);text(cs,"___________________________",60.5f,578,BODY_SIZE,PDType1Font.HELVETICA);text(cs,"BaluRaju P V, Proprietor",60.5f,593,BODY_SIZE,PDType1Font.HELVETICA);text(cs,"___________________________",303f,578,BODY_SIZE,PDType1Font.HELVETICA);text(cs,value(a.getCandidateFullName()),303f,593,BODY_SIZE,PDType1Font.HELVETICA);text(cs,"Date: "+value(a.getExecutionDate()),303f,615,BODY_SIZE,PDType1Font.HELVETICA);
        }
    }
    private float heading(PDPageContentStream cs,String s,float top)throws Exception{text(cs,s,LEFT,top,10.5f,PDType1Font.HELVETICA_BOLD);return top-22;}
    private float bullet(PDPageContentStream cs,String s,float top)throws Exception{return paragraph(cs,"•  "+s,78.1f,top,RIGHT,10f)-4;}
    private float paragraph(PDPageContentStream cs,String s,float x,float top,float right,float size)throws Exception{List<String> lines=wrap(s,right-x,size);float y=top;for(String line:lines){text(cs,line,x,y,size,PDType1Font.HELVETICA);y-=LEADING;}return y;}
    private List<String> wrap(String s,float maxWidth,float size)throws Exception{List<String> out=new ArrayList<>();String[] words=s.trim().split("\\s+");String line="";for(String word:words){String candidate=line.isEmpty()?word:line+" "+word;if(width(candidate,size)<=maxWidth||line.isEmpty())line=candidate;else{out.add(line);line=word;}}if(!line.isEmpty())out.add(line);return out;}
    private float width(String s,float size)throws Exception{return PDType1Font.HELVETICA.getStringWidth(s)/1000f*size;}
    private void centered(PDPageContentStream cs,String s,float top,float size,PDType1Font font)throws Exception{float x=(PAGE_W-font.getStringWidth(s)/1000f*size)/2f;text(cs,s,x,PAGE_H-top-size,size,font);}
    private void text(PDPageContentStream cs,String s,float x,float baseline,float size,PDType1Font font)throws Exception{cs.beginText();cs.setFont(font,size);cs.newLineAtOffset(x,PAGE_H-baseline);cs.showText(safePdf(s));cs.endText();}
    private String safe(String v){return v==null||v.isBlank()?"[Not provided]":v.trim();}
    private String value(String v){return safe(v);}
    private String idText(Agreement a){String type=a.getCandidateIdProofType(),ref=a.getCandidateIdProofReference();if((type==null||type.isBlank())&&(ref==null||ref.isBlank()))return "the identity information provided";if(ref==null||ref.isBlank())return safe(type);return safe(type)+" "+ref;}
    private String safePdf(String s){if(s==null)return "";return s.replace("\r"," ").replace("\n"," ").replace("₹","Rs.");}
}
