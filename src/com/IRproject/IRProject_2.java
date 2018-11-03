package com.IRproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;

public class IRProject_2 {
	private IndexWriter writer;
	private static int docsize;
	private static BufferedWriter filewriter;
	
	
	public static void printOutput(String method, ArrayList<String> terms, ArrayList<String> results, int comparisons) throws IOException {
		filewriter.write(method);
		filewriter.newLine();

		filewriter.write(String.join(" ", terms));
		/*System.out.println();
		System.out.println("Results: " + String.join(" ", results));
		System.out.println("Number of documents in results: " + results.size());
		System.out.println("Number of comparisons: " + comparisons);*/
		filewriter.newLine();
		if(results.isEmpty())
			filewriter.write("Results: " + "empty");
		else 
			filewriter.write("Results: " + String.join(" ", results));
		filewriter.newLine();
		filewriter.write("Number of documents in results: " + results.size());
		filewriter.newLine();
		filewriter.write("Number of comparisons: " + comparisons);
		filewriter.newLine();
		
	}
	
	
	public static void GetPostings(HashMap map, ArrayList<String> terms) throws IOException {
		for( int i=0; i< terms.size(); i++) {
			ArrayList<String> postings = (ArrayList<String>) map.get(terms.get(i));
			System.out.println("GetPostings");
			System.out.println(terms.get(i));
			System.out.println("Postings list: " + String.join(" ", postings));
			System.out.println("Postings size: " + postings.size());
			filewriter.write("GetPostings");
			filewriter.newLine();
			filewriter.write(terms.get(i));
			filewriter.newLine();
			filewriter.write("Postings list: " + String.join(" ", postings));
			filewriter.newLine();
		}
	}

	public static int findMax(HashMap map, ArrayList<String> terms) {
		int max = -1, index = 0;
		for( int i=0; i< terms.size(); i++) {
			ArrayList<String> postings = (ArrayList<String>) map.get(terms.get(i));
			if(postings.size() > max) {
				max = postings.size();
				index = i;
			}
		}
		return index;
	}
	
	public static int findMin(HashMap map, ArrayList<String> terms) {
		int min = 999999, index = 0;
		for( int i=0; i< terms.size(); i++) {
			ArrayList<String> postings = (ArrayList<String>) map.get(terms.get(i));
			if(postings.size() < min) {
				min = postings.size();
				index = i;
			}
		}
		return index;
	}
	
	public static void printMap(HashMap mp) {
	    Iterator it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	    	HashMap.Entry pair = (HashMap.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	public static ArrayList<String> FindComparisonsForOr(ArrayList<String> postings1, ArrayList<String> postings2, int a[]) {
		int i=0, j=0;
		ArrayList<String> temp = new ArrayList<String>(); 
		
		while(i < postings1.size() && j < postings2.size()) {
			if(Integer.parseInt(postings1.get(i)) < Integer.parseInt(postings2.get(j))){
				temp.add(postings1.get(i));
				i++;
			} else if(Integer.parseInt(postings1.get(i)) > Integer.parseInt(postings2.get(j))) {
				temp.add(postings2.get(j));
				j++;
			} else {
				temp.add(postings1.get(i));
				i++;
				j++;
			}
			a[0]++;
		}
		if(i < postings1.size())
			temp.addAll(postings1.subList(i, postings1.size()));
		if(j < postings2.size())
			temp.addAll(postings2.subList(j, postings2.size()));
			
		return temp;
	}
	public static boolean HasSkip(int size, int index) {
		
		int skip = (int) Math.sqrt(size);
		if(index % skip == 0 && (index + skip < size) && skip != 1)
			return true;
		
		
		return false;
		
	}
	
	
	public static void TAATOr(HashMap map, ArrayList<String> termsorig) throws IOException {
		ArrayList<String> postings = new ArrayList<>();
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> temp = new ArrayList<>();
		ArrayList<String> terms = new ArrayList<>();
		terms = (ArrayList<String>) termsorig.clone();
		int min, max;
		int[] comparison= {0};
		min = findMin(map, terms);
		result = (ArrayList<String>) map.get(terms.get(min));
		terms.remove(min);
		for(int i=0; i < terms.size(); i++) {
			result = FindComparisonsForOr(result,(ArrayList<String>) map.get(terms.get(i)), comparison);
		}
		printOutput("TaatOr", termsorig, result, comparison[0]);
	}
	
	public static ArrayList<String> FindComparisonsForAnd(ArrayList<String> postings1, ArrayList<String> postings2, int a[]) {
		int i=0, j=0;
		ArrayList<String> temp = new ArrayList<String>(); 
		int skip1 = (int) Math.sqrt(postings1.size());
		int skip2 = (int) Math.sqrt(postings2.size());
		while(i < postings1.size() && j < postings2.size()) {
			if(Integer.parseInt(postings1.get(i)) < Integer.parseInt(postings2.get(j))){
				if(HasSkip(postings1.size(), i) && Integer.parseInt(postings1.get(i + skip1)) <= Integer.parseInt(postings2.get(j))) {
					i += skip1;
					a[0]++;
				}
				else 
					i++;
			} else if(Integer.parseInt(postings1.get(i)) > Integer.parseInt(postings2.get(j))) {
				if(HasSkip(postings2.size(), j) && Integer.parseInt(postings1.get(i)) >= Integer.parseInt(postings2.get(j + skip2))) {
					j += skip2;
					a[0]++;
				}
				else 
					j++;
			} else {
				temp.add(postings1.get(i));
				i++;
				j++;
			}
			a[0]++;
		}
		
		return temp;
	}
	

	@SuppressWarnings("unchecked")
	public static void TAATAnd(HashMap map, ArrayList<String> termsorig) throws IOException {
		int i = 0;
		ArrayList<String> postings = new ArrayList<>();
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> temp = new ArrayList<>();
		ArrayList<String> terms = new ArrayList<>();
		terms = (ArrayList<String>) termsorig.clone();
		int min, max;
		int[] comparison= {0};
		min = findMin(map, terms);
		result = (ArrayList<String>) map.get(terms.get(min));
		terms.remove(min);
		while(terms.size() != 0) {
			max = findMax(map, terms);
			result = FindComparisonsForAnd(result,(ArrayList<String>) map.get(terms.get(max)), comparison);
			terms.remove(max);
		}
		printOutput("TaatAnd", termsorig, result, comparison[0]);
	}
	
	public static int MinIndex(ArrayList<Integer> vlist) {
		return vlist.indexOf(Collections.min(vlist));
		
	}
	
	public static void DAATOr(HashMap map, ArrayList<String> termsorig) throws IOException {
		ArrayList<String> postings = new ArrayList<>();
		ArrayList<Integer> ptr = new ArrayList<Integer>(Collections.nCopies(termsorig.size(), 0));
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> terms = new ArrayList<>();
		terms = (ArrayList<String>) termsorig.clone();
		int comparison= 0;
		ArrayList<ArrayList<String>> matrix = new ArrayList<ArrayList<String>>();
		for(int i=0; i< terms.size(); i++) {
			matrix.add((ArrayList<String>) map.get(terms.get(i)));
		}
		//System.out.println(matrix.size());
		while(!matrix.isEmpty()) {
			ArrayList<Integer> vlist = new ArrayList<>();
			for(int i=0; i< matrix.size(); i++) {
				postings = matrix.get(i);
				vlist.add(Integer.valueOf(postings.get(ptr.get(i))));
			}
			int min = MinIndex(vlist);
			comparison += vlist.size() - 1;
			int min_val = vlist.get(min);
			result.add(String.valueOf(vlist.get(min)));
/*			System.out.println("vlist = " + vlist);
			System.out.println("matrix = " + matrix);
			System.out.println("ptr = " + ptr);
			System.out.println("result = " + result);
			System.out.println("min = " + min + " val = " + vlist.get(min));*/
			for(int i=0; i< vlist.size(); i++) {

				if(vlist.get(i).equals(min_val)) {

					ptr.set(i, ptr.get(i)+1);
					if(ptr.get(i).equals(matrix.get(i).size())) {
						matrix.remove(i);
						vlist.remove(i);
						ptr.remove(i);
						i--;
					}
				}
			}
			/*System.out.println("vlist = " + vlist);
			System.out.println("matrix = " + matrix);
			System.out.println("ptr = " + ptr);
			System.out.println("result = " + result);*/
		}		
		printOutput("DaatOr", termsorig, result, comparison);
	}
	
	
	public static void DAATAndImpl(HashMap map, ArrayList<String> termsorig) throws IOException {
		ArrayList<String> postings = new ArrayList<>();
		ArrayList<Integer> ptr = new ArrayList<Integer>(Collections.nCopies(termsorig.size(), 0));
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> terms = new ArrayList<>();
		terms = (ArrayList<String>) termsorig.clone();
		int comparison= 0;
		boolean shouldBreak = false;
		ArrayList<ArrayList<String>> matrix = new ArrayList<ArrayList<String>>();
		for(int i=0; i< terms.size(); i++) {
			matrix.add((ArrayList<String>) map.get(terms.get(i)));
		}
		//System.out.println(matrix.size());
		while(!shouldBreak) {
			ArrayList<Integer> vlist = new ArrayList<>();
			for(int i=0; i< matrix.size(); i++) {
				postings = matrix.get(i);
				vlist.add(Integer.valueOf(postings.get(ptr.get(i))));
			}
			int min = MinIndex(vlist);
			comparison += vlist.size() - 1;
			int min_val = vlist.get(min);
			result.add(String.valueOf(vlist.get(min)));
/*			System.out.println("vlist = " + vlist);
			System.out.println("matrix = " + matrix);
			System.out.println("ptr = " + ptr);
			System.out.println("result = " + result);
			System.out.println("min = " + min + " val = " + vlist.get(min));*/
			for(int i=0; i< vlist.size(); i++) {

				if(vlist.get(i).equals(min_val)) {
					
					ptr.set(i, ptr.get(i)+1);
					if(ptr.get(i).equals(matrix.get(i).size())) {
						shouldBreak = true;
					}
				}
			}
			/*System.out.println("vlist = " + vlist);
			System.out.println("matrix = " + matrix);
			System.out.println("ptr = " + ptr);
			System.out.println("result = " + result);*/
		}		
		printOutput("DaatOr", termsorig, result, comparison);
	}
	

	public static void DAATAnd(HashMap map, ArrayList<String> termsorig) throws IOException {
		ArrayList<String> postings = new ArrayList<>();
		ArrayList<Integer> ptr = new ArrayList<Integer>(Collections.nCopies(termsorig.size()-1, 0));
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> temp = new ArrayList<>();
		ArrayList<String> terms = new ArrayList<>();
		terms = (ArrayList<String>) termsorig.clone();
		int min = findMin(map, terms);
		temp = (ArrayList<String>) map.get(terms.get(min));
		terms.remove(min);
		int comparison = 0;
		int found = 0;
		for(int i=0 ; i< temp.size(); i++) {
			found = 0;
			for(int j=0; j< terms.size(); j++) {
				postings = (ArrayList<String>) map.get(terms.get(j));
				int skip2 = (int) Math.sqrt(postings.size());
				int pos = ptr.get(j);
				while(pos < postings.size()) {
					comparison++;
					if(Integer.parseInt(temp.get(i)) > Integer.parseInt(postings.get(pos))) {
						if(HasSkip(postings.size(), pos) && Integer.parseInt(postings.get(pos + skip2)) <= Integer.parseInt(temp.get(i))) {
							pos += skip2;
							comparison++;
						} else
							pos++;
					} else if (Integer.parseInt(temp.get(i)) == Integer.parseInt(postings.get(pos))) {
						found++;
						pos++;
						break;
					} else {
						break;
					}
				}
				ptr.set(j, pos);
				
			}
			if(found == terms.size()) {
				result.add(temp.get(i));
			}
		}	

		printOutput("DaatAnd", termsorig, result, comparison);
	}
	
	
	public static HashMap Indexer(String folderloc) throws IOException {
		HashMap invertedList = new HashMap<String, ArrayList>();
		
		Path path = Paths.get(folderloc);
	    Directory oldDirectory = FSDirectory.open(path);
	    Directory rdir = new RAMDirectory((FSDirectory) oldDirectory, IOContext.DEFAULT);
	    IndexReader indexReader = DirectoryReader.open(rdir); 
	    System.out.print(indexReader.numDocs());
	    docsize = indexReader.numDocs();
	    Fields fields = MultiFields.getFields(indexReader);
	    Iterator fieldIterator = fields.iterator();
	    ArrayList<String> termslist = new ArrayList<>();
	    int i =0;
	    while(fieldIterator.hasNext()) {
	    	String field = (String) fieldIterator.next();
	    	if(field.equals("id"))
	    		continue;
	    	Terms terms = MultiFields.getTerms(indexReader, field);
	    	TermsEnum termsEnum = terms.iterator();
	    	while(termsEnum.next() != null) {
	    		i++;
	    		CharsRefBuilder spare = new CharsRefBuilder();
	    		ArrayList<String> postings = new ArrayList<>();
	    		BytesRef termString = termsEnum.term();
	    		PostingsEnum docsEnum = MultiFields.getTermDocsEnum(indexReader, field, termString);
	    		int docid = docsEnum.nextDoc();
	    		while(docid != docsEnum.NO_MORE_DOCS) {
	    			postings.add(String.valueOf(docid));
	    			docid = docsEnum.nextDoc();
	    		}
	    		spare.copyUTF8Bytes(termString);
	    		//System.out.println(spare.toString());
	    		//System.out.println(postings);
	    		termslist.add(termString.utf8ToString());
	    		invertedList.put(termString.utf8ToString(), postings);	
	    		//System.out.println("--------------------------------------");
	    	}
	    }
	    indexReader.close(); 
	    //System.out.println(invertedList.size());
	    //System.out.println(termslist.size());
	    //printMap(invertedList);
	    return invertedList;
	  
	}

	public static void main(String[] args)  {
		// TODO Auto-generated method stub
		HashMap invertedList;
		ArrayList<String> terms = new ArrayList<>();
		try {
			invertedList = Indexer(args[0]);

			BufferedReader br = new BufferedReader(new FileReader(args[2])); 
			filewriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1])));
			 String st; 
			  while ((st = br.readLine()) != null) {
				terms = new ArrayList( Arrays.asList(st.split(" ")));
				GetPostings(invertedList, terms);
				TAATAnd(invertedList, terms);
				TAATOr(invertedList, terms);
				DAATAnd(invertedList, terms);
				DAATOr(invertedList, terms);
			  }

			filewriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
}
